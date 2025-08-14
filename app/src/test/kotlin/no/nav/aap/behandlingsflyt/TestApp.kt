package no.nav.aap.behandlingsflyt

import com.papsign.ktor.openapigen.route.apiRouting
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.YtelseTypeCode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.gateway.SamhandlerForholdDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.gateway.SamhandlerYtelseDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.gateway.TjenestePensjonRespons
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.gateway.TpOrdning
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjonstype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Oppholdstype
import no.nav.aap.behandlingsflyt.integrasjon.institusjonsopphold.InstitusjonsoppholdJSON
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.integrasjon.defaultGatewayProvider
import no.nav.aap.behandlingsflyt.integrasjon.ident.PdlIdentGateway
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Ident
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.KlageV0
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.ManueltOppgittBarn
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.OppgitteBarn
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadMedlemskapDto
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadStudentDto
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadV0
import no.nav.aap.behandlingsflyt.prosessering.HendelseMottattHåndteringJobbUtfører
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
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.verdityper.dokument.JournalpostId
import no.nav.aap.verdityper.dokument.Kanal
import org.slf4j.LoggerFactory
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import javax.sql.DataSource

private val log = LoggerFactory.getLogger("TestApp")

// Kjøres opp for å få logback i console uten json
fun main() {
    System.setProperty("NAIS_CLUSTER_NAME", "LOCAL")
    val postgres = postgreSQLContainer()

    AzurePortHolder.setPort(8081)
    FakeServers.start() // azurePort = 8081)

    // Starter server
    embeddedServer(Netty, configure = {
        shutdownTimeout = TimeUnit.SECONDS.toMillis(20)
        connector {
            port = 8080
        }
    }) {
        val dbConfig = DbConfig(
            url = postgres.jdbcUrl,
            username = postgres.username,
            password = postgres.password
        )
        // Useful for connecting to the test database locally
        // jdbc URL contains the host and port and database name.
        println("jdbcUrl: ${postgres.jdbcUrl}. Password: ${postgres.password}. Username: ${postgres.username}.")
        server(dbConfig, postgresRepositoryRegistry, defaultGatewayProvider())

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
    val erStudent = if (dto.student) "JA" else "NEI"
    val harYrkesskade = if (dto.yrkesskade) "JA" else "NEI"

    val oppgitteBarn = if (urelaterteBarn.isNotEmpty()) {
        OppgitteBarn(
            barn = urelaterteBarn
                .map {
                    ManueltOppgittBarn(
                        ident = it.identer.filter { it.aktivIdent }.map { Ident(it.identifikator) }.firstOrNull(),
                        fødselsdato = it.fødselsdato.toLocalDate(),
                        navn = it.navn.fornavn + " " + it.navn.etternavn,
                        relasjon = ManueltOppgittBarn.Relasjon.FORELDER
                    )
                },
            identer = setOf()
        )
    } else {
        log.info("Oppretter ikke oppgitte barn siden det ikke er noen urelatert barn i testcase")
        null
    }
    val harMedlemskap = if (dto.medlemskap) "JA" else "NEI"
    return SøknadV0(
        student = SøknadStudentDto(erStudent), harYrkesskade, oppgitteBarn,
        medlemskap = SøknadMedlemskapDto(harMedlemskap, null, null, null, listOf()),
    )
}

private fun sendInnSøknad(datasource: DataSource, dto: OpprettTestcaseDTO): Sak {
    val ident = genererIdent(dto.fødselsdato)
    val barn = dto.barn.filter { it.harRelasjon }.map { genererBarn(it) }
    val urelaterteBarnIPDL = dto.barn.filter { !it.harRelasjon && it.skalFinnesIPDL }.map { genererBarn(it) }
    val urelaterteBarnIkkeIPDL = dto.barn.filter { !it.harRelasjon && !it.skalFinnesIPDL }.map { genererBarn(it) }
    barn.forEach { FakePersoner.leggTil(it) }
    urelaterteBarnIPDL.forEach { FakePersoner.leggTil(it) }
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
            PdlIdentGateway(),
            PersonRepositoryImpl(connection),
            SakRepositoryImpl(connection)
        )
        val sak = sakService.finnEllerOpprett(ident, periode)

        val flytJobbRepository = FlytJobbRepository(connection)

        val melding = mapTilSøknad(dto, urelaterteBarnIkkeIPDL + urelaterteBarnIPDL)

        flytJobbRepository.leggTil(
            HendelseMottattHåndteringJobbUtfører.nyJobb(
                sakId = sak.id,
                dokumentReferanse = InnsendingReferanse(JournalpostId("" + System.currentTimeMillis())),
                brevkategori = InnsendingType.SØKNAD,
                kanal = Kanal.DIGITAL,
                melding = melding,
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
                melding = KlageV0(
                    kravMottatt = LocalDate.now().minusWeeks(1),
                    skalOppretteNyBehandling = true
                ),
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
    val envPort = System.getenv("POSTGRES_PORT")?.toIntOrNull()
    if (envPort != null) {
        postgres.withExposedPorts(5432)
        postgres.setPortBindings(listOf("$envPort:5432"))
    }
    postgres.waitingFor(HostPortWaitStrategy().withStartupTimeout(Duration.of(60L, ChronoUnit.SECONDS)))
    postgres.start()
    return postgres
}