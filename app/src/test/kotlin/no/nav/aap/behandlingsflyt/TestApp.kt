package no.nav.aap.behandlingsflyt

import com.papsign.ktor.openapigen.route.apiRouting
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.response.respondWithStatus
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.HttpStatusCode
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.YtelseTypeCode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.gateway.SamhandlerForholdDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.gateway.SamhandlerYtelseDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.gateway.TjenestePensjonRespons
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.gateway.TpOrdning
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjonstype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Oppholdstype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.integrasjon.defaultGatewayProvider
import no.nav.aap.behandlingsflyt.integrasjon.ident.PdlIdentGateway
import no.nav.aap.behandlingsflyt.integrasjon.institusjonsopphold.InstitusjonsoppholdJSON
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Ident
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.ManueltOppgittBarn
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.OppgitteBarn
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.StudentStatus
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadMedlemskapDto
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadStudentDto
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.SøknadV0
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.prosessering.HendelseMottattHåndteringJobbUtfører
import no.nav.aap.behandlingsflyt.prosessering.ProsesseringsJobber
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate.SaksnummerParameter
import no.nav.aap.behandlingsflyt.test.AzurePortHolder
import no.nav.aap.behandlingsflyt.test.FakePersoner
import no.nav.aap.behandlingsflyt.test.FakeServers
import no.nav.aap.behandlingsflyt.test.LokalUnleash
import no.nav.aap.behandlingsflyt.test.modell.TestPerson
import no.nav.aap.behandlingsflyt.test.modell.TestYrkesskade
import no.nav.aap.behandlingsflyt.test.modell.defaultInntekt
import no.nav.aap.behandlingsflyt.test.modell.genererIdent
import no.nav.aap.behandlingsflyt.test.testGatewayProvider
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.komponenter.verdityper.Tid
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.testutil.ManuellMotorImpl
import no.nav.aap.verdityper.dokument.JournalpostId
import no.nav.aap.verdityper.dokument.Kanal
import org.slf4j.LoggerFactory
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy
import org.testcontainers.postgresql.PostgreSQLContainer
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import javax.sql.DataSource
import kotlin.random.Random

private val log = LoggerFactory.getLogger("TestApp")
lateinit var testScenarioOrkestrator: TestScenarioOrkestrator
lateinit var motor: ManuellMotorImpl
lateinit var datasource: DataSource

// Kjøres opp for å få logback i console uten json
fun main() {
    val dbConfig = initDbConfig()

    AzurePortHolder.setPort(8081)
    FakeServers.start() // azurePort = 8081)

    // Starter server
    embeddedServer(Netty, configure = {
        shutdownTimeout = TimeUnit.SECONDS.toMillis(20)
        connector {
            port = 8080
        }
    }) {
        val gatewayProvider = testGatewayProvider(LokalUnleash::class)

        // Useful for connecting to the test database locally
        // jdbc URL contains the host and port and database name.
        server(dbConfig, postgresRepositoryRegistry, gatewayProvider)

        datasource = initDatasource(dbConfig)
        motor = lazy {
            ManuellMotorImpl(
                datasource,
                jobber = ProsesseringsJobber.alle(),
                repositoryRegistry = postgresRepositoryRegistry,
                gatewayProvider
            )
        }.value

        testScenarioOrkestrator = TestScenarioOrkestrator(gatewayProvider, datasource, motor)

        apiRouting {
            route("/test") {
                route("/opprett") {
                    post<Unit, OpprettTestcaseDTO, OpprettTestcaseDTO> { _, dto ->
                        opprettNySakOgBehandling(dto)
                        respond(dto)
                    }
                }

                route("/endre/{saksnummer}/legg-til-institusjonsopphold") {
                    post<SaksnummerParameter, Unit, LeggTilInstitusjonsoppholdDTO> { param, dto ->
                        val ident = hentIdentForSak(Saksnummer(param.saksnummer))

                        val oppdatertPerson = FakePersoner.hentPerson(ident)
                            ?.medInstitusjonsopphold(listOf(genererInstitusjonsopphold(dto)))

                        if (oppdatertPerson != null) {
                            FakePersoner.oppdater(oppdatertPerson)
                            respondWithStatus(HttpStatusCode.OK)
                        } else {
                            log.warn("Finner ikke person med ident $ident for å legge til institusjonsopphold")
                            respondWithStatus(HttpStatusCode.BadRequest)
                        }
                    }

                }
            }
        }

    }.start(wait = true)
}

private fun initDbConfig(): DbConfig {
    return if (System.getenv("NAIS_DATABASE_BEHANDLINGSFLYT_BEHANDLINGSFLYT_JDBC_URL").isNullOrBlank()) {
        val postgres = postgreSQLContainer()

        DbConfig(
            url = postgres.jdbcUrl,
            username = postgres.username,
            password = postgres.password
        )
    } else {
        DbConfig()
    }.also {
        println("----\nDATABASE URL: \n${it.url}?user=${it.username}&password=${it.password}\n----")
    }
}

private fun genererInstitusjonsopphold(dto: LeggTilInstitusjonsoppholdDTO) = InstitusjonsoppholdJSON(
    organisasjonsnummer = Random.nextInt(911111111, 999999999).toString(),
    kategori = dto.oppholdstype.name,
    institusjonstype = dto.institusjonstype.name,
    forventetSluttdato = dto.oppholdTom,
    startdato = dto.oppholdFom,
    institusjonsnavn = "Test Institusjon"
)

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
    )
}

private fun mapTilSøknad(dto: OpprettTestcaseDTO, urelaterteBarn: List<TestPerson>): SøknadV0 {
    val erStudent = if (dto.student) StudentStatus.Ja else StudentStatus.Nei
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
            identer = emptySet()
        )
    } else {
        log.info("Oppretter ikke oppgitte barn siden det ikke er noen urelatert barn i testcase")
        null
    }
    val harMedlemskap = if (dto.medlemskap) "JA" else "NEI"
    return SøknadV0(
        student = SøknadStudentDto(erStudent), harYrkesskade, oppgitteBarn,
        medlemskap = SøknadMedlemskapDto(harMedlemskap, null, null, null, emptyList()),
    )
}

private fun sendInnSøknad(dto: OpprettTestcaseDTO): Sak {
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
            yrkesskade = if (dto.yrkesskade) listOf(
                TestYrkesskade(),
                TestYrkesskade(skadedato = null, saksreferanse = "ABCDE")
            ) else emptyList(),
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
        Tid.MAKS
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

private fun opprettNySakOgBehandling(dto: OpprettTestcaseDTO): Sak {
    val sak = sendInnSøknad(dto)

    if (dto.steg in listOf(StegType.START_BEHANDLING, StegType.AVKLAR_STUDENT)) return sak

    motor.kjørJobber()

    // fullfør førstegangsbehandling
    log.info("Fullfører førstegangsbehandling for sak ${sak.id}")
    val behandling = hentSisteBehandlingForSak(sak.id)

    with(testScenarioOrkestrator) {
        // Student eller sykdom
        if (dto.student) {
            løsStudent(behandling)
        } else {
            if (dto.steg == StegType.AVKLAR_SYKDOM) return sak
            løsSykdom(
                behandling = behandling,
                vurderingGjelderFra = dto.søknadsdato ?: sak.rettighetsperiode.fom,
                erArbeidsevnenNedsatt = dto.erArbeidsevnenNedsatt,
                erNedsettelseIArbeidsevneMerEnnHalvparten = dto.erNedsettelseIArbeidsevneMerEnnHalvparten
            )
        }

        val harBehandlingsgrunnlag = dto.erArbeidsevnenNedsatt && dto.erNedsettelseIArbeidsevneMerEnnHalvparten

        if (harBehandlingsgrunnlag) {
            if (dto.steg == StegType.VURDER_BISTANDSBEHOV) return sak
            løsBistand(behandling, dto.søknadsdato ?: sak.rettighetsperiode.fom)

            // Vurderinger i sykdom
            if (dto.steg == StegType.REFUSJON_KRAV) return sak
            løsRefusjonskrav(behandling)
        }

        if (dto.steg == StegType.SYKDOMSVURDERING_BREV) return sak
        else if (!dto.student) løsSykdomsvurderingBrev(behandling)

        if (dto.steg == StegType.KVALITETSSIKRING) return sak
        kvalitetssikreOk(behandling)

        if (harBehandlingsgrunnlag) {
            // Yrkesskade
            if (dto.yrkesskade) {
                if (dto.steg == StegType.VURDER_YRKESSKADE) return sak
                løsYrkesSkade(behandling)
            }

            // Inntekt
            if (dto.steg == StegType.FASTSETT_BEREGNINGSTIDSPUNKT) return sak
            løsBeregningstidspunkt(behandling)

            if (dto.inntekterPerAr == null || dto.inntekterPerAr.isEmpty()) {
                if (dto.steg == StegType.MANGLENDE_LIGNING) return sak
                løsManuellInntektVurdering(behandling)
            }

            // Forutgående medlemskap
            if (dto.yrkesskade) {
                løsFastsettYrkesskadeInntekt(behandling)
            } else {
                if (dto.steg == StegType.VURDER_MEDLEMSKAP) return sak
                løsForutgåendeMedlemskap(behandling)
            }

            // Oppholdskrav
            if (dto.steg == StegType.VURDER_OPPHOLDSKRAV) return sak
            løsOppholdskrav(behandling)

            // Institusjonsopphold
            if (dto.steg == StegType.DU_ER_ET_ANNET_STED) return sak
            if (dto.institusjoner.fengsel == true) {
                løsSoningsforhold(behandling)
            }

            // Barnetillegg
            if (dto.steg == StegType.BARNETILLEGG) return sak
            if (dto.barn.isNotEmpty()) {
                løsBarnetillegg(behandling)
            }

            // Samordning
            if (dto.steg == StegType.SAMORDNING_GRADERING) return sak
            if (dto.sykepenger.isEmpty()) {
                løsUtenSamordning(behandling)
            } else {
                løsSamordning(behandling, dto.sykepenger)
            }

            if (dto.steg == StegType.SAMORDNING_ANDRE_STATLIGE_YTELSER) return sak
            løsSamordningAndreStatligeYtelser(behandling)

            if (dto.tjenestePensjon == true) {
                if (dto.steg == StegType.SAMORDNING_TJENESTEPENSJON_REFUSJONSKRAV) return sak
                løsTjenestepensjonRefusjonskravVurdering(behandling)
            }

            // Vedtak
            if (dto.steg == StegType.FORESLÅ_VEDTAK) return sak
            løsForeslåVedtakLøsning(behandling)
        }

        if (dto.steg == StegType.FATTE_VEDTAK) return sak
        fattVedtakEllerSendRetur(behandling)

        if (dto.steg == StegType.BREV) return sak
        else if (harBehandlingsgrunnlag) løsVedtaksbrev(behandling)
        else løsVedtaksbrev(behandling, TypeBrev.VEDTAK_AVSLAG)

        return sak
    }
}

private fun hentIdentForSak(saksnummer: Saksnummer): String {
    return datasource.transaction { connection ->
        val repositoryProvider = postgresRepositoryRegistry.provider(connection)
        val sakRepository = repositoryProvider.provide<SakRepository>()
        val sak = sakRepository.hent(saksnummer)

        sak.person.aktivIdent().identifikator
    }
}

private fun hentSisteBehandlingForSak(sakId: SakId): Behandling {
    return datasource.transaction { connection ->
        val repositoryProvider = postgresRepositoryRegistry.provider(connection)
        val sbService = SakOgBehandlingService(
            repositoryProvider,
            defaultGatewayProvider()
        )

        val behandling = sbService.finnSisteYtelsesbehandlingFor(sakId)
        requireNotNull(behandling) { "Finner ikke behandling for sakId: $sakId" }
        behandling
    }
}

internal fun postgreSQLContainer(): PostgreSQLContainer {
    val postgres = PostgreSQLContainer("postgres:16")
        .apply {
            val envPort = System.getenv("POSTGRES_PORT")?.toIntOrNull()
            if (envPort != null) {
                withExposedPorts(5432)
                portBindings = listOf("$envPort:5432")
            }
            withLogConsumer(Slf4jLogConsumer(log))
            waitingFor(HostPortWaitStrategy().withStartupTimeout(Duration.of(60L, ChronoUnit.SECONDS)))
        }
    postgres.start() // venter til container er klar
    return postgres
}
