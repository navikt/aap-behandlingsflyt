package no.nav.aap.behandlingsflyt

import com.papsign.ktor.openapigen.route.apiRouting
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import no.nav.aap.behandlingsflyt.behandling.brev.SignaturService
import no.nav.aap.behandlingsflyt.integrasjon.brev.BrevGateway
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingService
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.YtelseTypeCode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.gateway.SamhandlerForholdDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.gateway.SamhandlerYtelseDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.gateway.TjenestePensjonRespons
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.gateway.TpOrdning
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.Ytelser
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjonstype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Oppholdstype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.adapter.InstitusjonsoppholdJSON
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.integrasjon.ident.PdlIdentGateway
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Ident
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.OppgitteBarn
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadMedlemskapDto
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadStudentDto
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadV0
import no.nav.aap.behandlingsflyt.prosessering.HendelseMottattHåndteringJobbUtfører
import no.nav.aap.behandlingsflyt.repository.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.behandling.brev.bestilling.BrevbestillingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.test.AzurePortHolder
import no.nav.aap.behandlingsflyt.test.FakePersoner
import no.nav.aap.behandlingsflyt.test.FakeServers
import no.nav.aap.behandlingsflyt.test.modell.TestPerson
import no.nav.aap.behandlingsflyt.test.modell.TestYrkesskade
import no.nav.aap.behandlingsflyt.test.modell.defaultInntekt
import no.nav.aap.behandlingsflyt.test.modell.genererIdent
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.verdityper.dokument.JournalpostId
import no.nav.aap.verdityper.dokument.Kanal
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*
import javax.sql.DataSource

// Kjøres opp for å få logback i console uten json
fun main() {
    val postgres = postgreSQLContainer()

    AzurePortHolder.setPort(8081)
    FakeServers.start() // azurePort = 8081)

    // Starter server
    embeddedServer(Netty, port = 8080, watchPaths = listOf("classes")) {
        val dbConfig = DbConfig(
            url = postgres.jdbcUrl,
            username = postgres.username,
            password = postgres.password
        )
        // Useful for connecting to the test database locally
        // jdbc URL contains the host and port and database name.
        println("jdbcUrl: ${postgres.jdbcUrl}. Password: ${postgres.password}. Username: ${postgres.username}.")
        server(dbConfig, postgresRepositoryRegistry)
        System.setProperty("NAIS_CLUSTER_NAME", "LOCAL")

        val datasource = initDatasource(dbConfig)

        opprettTestKlage(datasource, alderIkkeOppfyltTestCase)

        apiRouting {
            route("/test") {
                route("/opprett") {
                    post<Unit, OpprettTestcaseDTO, OpprettTestcaseDTO> { _, dto ->
                        sendInnSøknad(datasource, dto)
                        respond(dto)
                    }
                }
                route("/brev") {
                    post<Unit, String, TestBestillBrev> { _, dto ->
                        datasource.transaction { connection ->
                            val behandlingRepository = BehandlingRepositoryImpl(connection)
                            val avklaringsbehovRepository = AvklaringsbehovRepositoryImpl(connection)
                            val sakRepository = SakRepositoryImpl(connection)
                            val behandling = behandlingRepository.hent(dto.behandlingReferanse)
                            if (behandling.status().erAvsluttet()) {
                                throw IllegalStateException("Kan ikke legge på brevbehov på en avsluttet behandling")
                            }
                            val brevbestillingService = BrevbestillingService(
                                SignaturService(avklaringsbehovRepository),
                                BrevGateway(),
                                BrevbestillingRepositoryImpl(connection),
                                behandlingRepository,
                                sakRepository
                            )
                            brevbestillingService.bestill(
                                behandling.id,
                                TypeBrev.VEDTAK_INNVILGELSE,
                                UUID.randomUUID().toString()
                            )
                            val avklaringsbehovene =
                                AvklaringsbehovRepositoryImpl(connection).hentAvklaringsbehovene(behandling.id)

                            avklaringsbehovene.leggTil(listOf(Definisjon.SKRIV_BREV), behandling.aktivtSteg())
                        }

                        respond("OK")
                    }
                }
            }
        }

    }.start(wait = true)
}

private fun genererFengselsopphold() = InstitusjonsoppholdJSON(
    organisasjonsnummer = "12345",
    kategori = Oppholdstype.S.name,
    institusjonstype = Institusjonstype.FO.name,
    forventetSluttdato = LocalDate.now().plusYears(1),
    startdato = LocalDate.now().minusYears(2),
    institusjonsnavn = "Azkaban"
)

private fun genererSykehusopphold() = InstitusjonsoppholdJSON(
    organisasjonsnummer = "12345",
    kategori = Oppholdstype.H.name,
    institusjonstype = Institusjonstype.HS.name,
    forventetSluttdato = LocalDate.now().plusYears(1),
    startdato = LocalDate.now().minusYears(2),
    institusjonsnavn = "St. Mungos Hospital"
)

private fun genererBarn(dto: TestBarn): TestPerson {
    val ident = genererIdent(dto.fodselsdato)

    return TestPerson(
        identer = setOf(ident),
        fødselsdato = Fødselsdato(dto.fodselsdato),
        yrkesskade = emptyList()
    )
}

fun mapTilSøknad(dto: OpprettTestcaseDTO, urelaterteBarn: List<TestPerson>): SøknadV0 {
    val erStudent = if (dto.student) {
        "JA"
    } else {
        "NEI"
    }
    val harYrkesskade = if (dto.yrkesskade) {
        "JA"
    } else {
        "NEI"
    }
    val oppgitteBarn = if (urelaterteBarn.isNotEmpty()) {
        OppgitteBarn(identer = urelaterteBarn.flatMap { it.identer.filter { it.aktivIdent } }
            .map { Ident(it.identifikator) }.toSet())
    } else {
        null
    }
    val harMedlemskap = if (dto.medlemskap) {
        "JA"
    } else {
        "NEI"
    }
    return SøknadV0(
        student = SøknadStudentDto(erStudent), harYrkesskade, oppgitteBarn,
        medlemskap = SøknadMedlemskapDto(harMedlemskap, null, null, null, listOf())
    )
}

private fun sendInnSøknad(datasource: DataSource, dto: OpprettTestcaseDTO): Sak {
    val ident = genererIdent(dto.fødselsdato)
    val barn = dto.barn.filter { it.harRelasjon }.map { genererBarn(it) }
    val urelaterteBarn = dto.barn.filter { !it.harRelasjon }.map { genererBarn(it) }
    val tjenestePensjon = dto.tjenestePensjon
    barn.forEach { FakePersoner.leggTil(it) }
    urelaterteBarn.forEach { FakePersoner.leggTil(it) }
    FakePersoner.leggTil(
        TestPerson(
            identer = setOf(ident),
            fødselsdato = Fødselsdato(dto.fødselsdato),
            yrkesskade = if (dto.yrkesskade) listOf(TestYrkesskade()) else emptyList(),
            uføre = dto.uføre?.let(::Prosent),
            barn = barn,
            institusjonsopphold = listOfNotNull(
                if (dto.institusjoner.fengsel == true) genererFengselsopphold() else null,
                if (dto.institusjoner.sykehus == true) genererSykehusopphold() else null,
            ),
            inntekter = dto.inntekterPerAr?.map { inn -> inn.to() } ?: defaultInntekt(),
            sykepenger = dto.sykepenger.map {
                TestPerson.Sykepenger(
                    grad = it.grad,
                    periode = it.periode
                )
            },
            tjenestePensjon = if (dto.tjenestePensjon != null && dto.tjenestePensjon) TjenestePensjonRespons(
                fnr = ident.identifikator,
                forhold = listOf(
                    SamhandlerForholdDto(
                        TpOrdning(
                            "test",
                            "test",
                            "test"
                        ),
                        ytelser = listOf(
                            SamhandlerYtelseDto(
                                null,
                                YtelseTypeCode.ALDER,
                                LocalDate.now().minusYears(1),
                                null,
                                12345678L
                            )
                        )
                    )
                )
            ) else null,
        )
    )
    val periode = Periode(
        LocalDate.now(),
        LocalDate.now().plusYears(1).minusDays(1)
    )
    val sak = datasource.transaction { connection ->
        val sakService = PersonOgSakService(
            PdlIdentGateway,
            PersonRepositoryImpl(connection),
            SakRepositoryImpl(connection)
        )
        val sak = sakService.finnEllerOpprett(ident, periode)

        val flytJobbRepository = FlytJobbRepository(connection)

        flytJobbRepository.leggTil(
            HendelseMottattHåndteringJobbUtfører.nyJobb(
                sakId = sak.id,
                dokumentReferanse = InnsendingReferanse(JournalpostId("" + System.currentTimeMillis())),
                brevkategori = InnsendingType.SØKNAD,
                kanal = Kanal.DIGITAL,
                melding = mapTilSøknad(dto, urelaterteBarn),
                mottattTidspunkt = dto.søknadsdato?.atStartOfDay() ?: LocalDateTime.now(),
            )
        )
        sak
    }

    return sak
}

private fun sendInnKlage(datasource: DataSource, sak: Sak) {
    datasource.transaction { connection ->
        val flytJobbRepository = FlytJobbRepository(connection)

        flytJobbRepository.leggTil(
            HendelseMottattHåndteringJobbUtfører.nyJobb(
                sakId = sak.id,
                dokumentReferanse = InnsendingReferanse(JournalpostId("" + System.currentTimeMillis())),
                brevkategori = InnsendingType.KLAGE,
                kanal = Kanal.DIGITAL,
                mottattTidspunkt = LocalDateTime.now(),
            )
        )
    }
}

private fun opprettTestKlage(datasource: DataSource, testcaseDTO: OpprettTestcaseDTO) {
    val sak = sendInnSøknad(datasource, testcaseDTO)
    sendInnKlage(datasource, sak)
}

private val alderIkkeOppfyltTestCase = OpprettTestcaseDTO(
    fødselsdato = LocalDate.now().minusYears(17),
    barn = emptyList(),
    yrkesskade = false,
    uføre = null,
    institusjoner = Institusjoner(fengsel = false, sykehus = false),
    inntekterPerAr = null,
    søknadsdato = LocalDate.now(),
    student = false,
    medlemskap = true,
)

internal fun postgreSQLContainer(): PostgreSQLContainer<Nothing> {
    val postgres = PostgreSQLContainer<Nothing>("postgres:16")
    postgres.waitingFor(HostPortWaitStrategy().withStartupTimeout(Duration.of(60L, ChronoUnit.SECONDS)))
    postgres.start()
    return postgres
}