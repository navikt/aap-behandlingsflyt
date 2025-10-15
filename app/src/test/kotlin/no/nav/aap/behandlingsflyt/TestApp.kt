package no.nav.aap.behandlingsflyt

import com.papsign.ktor.openapigen.route.apiRouting
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSamordningGraderingLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklaringsbehovLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.ForeslåVedtakLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.SkrivBrevAvklaringsbehovLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.SkrivVedtaksbrevLøsning
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev
import no.nav.aap.behandlingsflyt.behandling.samordning.Ytelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.YtelseTypeCode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.gateway.SamhandlerForholdDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.gateway.SamhandlerYtelseDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.gateway.TjenestePensjonRespons
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.gateway.TpOrdning
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjonstype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Oppholdstype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.samordning.SamordningVurderingData
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.samordning.VurderingerForSamordning
import no.nav.aap.behandlingsflyt.integrasjon.defaultGatewayProvider
import no.nav.aap.behandlingsflyt.integrasjon.ident.PdlIdentGateway
import no.nav.aap.behandlingsflyt.integrasjon.institusjonsopphold.InstitusjonsoppholdJSON
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
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
import no.nav.aap.behandlingsflyt.prosessering.ProsesseringsJobber
import no.nav.aap.behandlingsflyt.repository.behandling.brev.bestilling.BrevbestillingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.test.AzurePortHolder
import no.nav.aap.behandlingsflyt.test.FakePersoner
import no.nav.aap.behandlingsflyt.test.FakeServers
import no.nav.aap.behandlingsflyt.test.modell.TestPerson
import no.nav.aap.behandlingsflyt.test.modell.TestYrkesskade
import no.nav.aap.behandlingsflyt.test.modell.defaultInntekt
import no.nav.aap.behandlingsflyt.test.modell.genererIdent
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.testutil.ManuellMotorImpl
import no.nav.aap.verdityper.dokument.JournalpostId
import no.nav.aap.verdityper.dokument.Kanal
import org.slf4j.LoggerFactory
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.TimeUnit
import javax.sql.DataSource

private val log = LoggerFactory.getLogger("TestApp")
lateinit var testScenarioOrkestrator: TestScenarioOrkestrator
lateinit var motor: ManuellMotorImpl
lateinit var datasource: DataSource

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

        val gatewayProvider = defaultGatewayProvider()

        // Useful for connecting to the test database locally
        // jdbc URL contains the host and port and database name.
        println("jdbcUrl: ${postgres.jdbcUrl}. Password: ${postgres.password}. Username: ${postgres.username}.")
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

        opprettTestKlage(alderIkkeOppfyltTestCase)

        apiRouting {
            route("/test") {
                route("/opprett") {
                    post<Unit, OpprettTestcaseDTO, OpprettTestcaseDTO> { _, dto ->
                        sendInnSøknad(dto)
                        respond(dto)
                    }
                }
            }
        }

        apiRouting {
            route("/test") {
                route("/opprett-og-fullfoer") {
                    post<Unit, OpprettTestcaseDTO, OpprettTestcaseDTO> { _, dto ->
                        sendInnOgFullførFørstegangsbehandling(dto)
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

private fun sendInnOgFullførFørstegangsbehandling(dto: OpprettTestcaseDTO): Sak {
    val sak = sendInnSøknad(dto)
    motor.kjørJobber()

    // fullfør førstegangsbehandling
    log.info("Fullfører førstegangsbehandling for sak ${sak.id}")
    val behandling = hentSisteBehandlingForSak(sak.id)

    if (dto.student) {
        behandling.løsStudent()
    }

    if (!dto.student) {
        behandling.løsSykdom()
            .løsBistand()
    }

    behandling.løsRefusjonskrav()
        .løsSykdomsvurderingBrev()
        .kvalitetssikreOk()

    if (dto.yrkesskade) {
        behandling.løsYrkesSkade()
    }

    behandling.løsBeregningstidspunkt()

    if (dto.inntekterPerAr == null || dto.inntekterPerAr.isEmpty()) {
        behandling.løsManuellInntektVurdering()
    }

    if (dto.yrkesskade) {
        behandling.løsFastsettYrkesskadeInntekt()
    } else {
        behandling.løsForutgåendeMedlemskap()
    }

    if (dto.institusjoner.fengsel == true) {
        behandling.løsSoningsforhold()
    }

    if (dto.barn.isNotEmpty() && !dto.barn.all { it.harRelasjon }) {
        behandling.løsBarnetillegg()
    }

    if (dto.sykepenger.isEmpty()) {
        behandling.løsUtenSamordning()
    } else {
        behandling.løsSamordning(dto.sykepenger.first().periode)
    }

    if (dto.tjenestePensjon != null && dto.tjenestePensjon) {
        behandling.løsTjenestepensjonRefusjonskravVurdering()
    }

    behandling.løsAvklaringsBehov(ForeslåVedtakLøsning())
        .fattVedtakEllerSendRetur()
        .løsVedtaksbrev()

    return sak
}

fun Behandling.løsVedtaksbrev(typeBrev: TypeBrev = TypeBrev.VEDTAK_INNVILGELSE): Behandling {
    val brevbestilling = hentBrevAvType(this, typeBrev)

    return this.løsAvklaringsBehov(vedtaksbrevLøsning(brevbestilling.referanse.brevbestillingReferanse))
}

private fun vedtaksbrevLøsning(brevbestillingReferanse: UUID): AvklaringsbehovLøsning {
    return SkrivVedtaksbrevLøsning(
        brevbestillingReferanse = brevbestillingReferanse,
        handling = SkrivBrevAvklaringsbehovLøsning.Handling.FERDIGSTILL
    )
}

private fun hentBrevAvType(behandling: Behandling, typeBrev: TypeBrev) =
    datasource.transaction(readOnly = true) {
        val brev = BrevbestillingRepositoryImpl(it).hent(behandling.id)
        brev.firstOrNull { it.typeBrev == typeBrev }
            ?: error("Ingen brev av type $typeBrev. Følgende finnes: ${brev.joinToString { it.typeBrev.toString() }}")
    }

fun hentSisteBehandlingForSak(sakId: SakId): Behandling {
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

private fun sendInnKlage(sak: Sak) {
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

@JvmName("løsStudentExt")
fun Behandling.løsStudent(): Behandling {
    return testScenarioOrkestrator.løsStudent(this)
}

@JvmName("løsSykdomExt")
fun Behandling.løsSykdom(
    vurderingGjelderFra: LocalDate? = null,
    erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense: Boolean? = null
): Behandling {
    return testScenarioOrkestrator.løsSykdom(
        this,
        vurderingGjelderFra = vurderingGjelderFra,
        erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense
    )
}

@JvmName("løsSykdomsvurderingBrevExt")
fun Behandling.løsSykdomsvurderingBrev(): Behandling {
    return testScenarioOrkestrator.løsSykdomsvurderingBrev(this)
}

@JvmName("løsYrkesSkadeExt")
fun Behandling.løsYrkesSkade(): Behandling {
    return testScenarioOrkestrator.løsYrkesSkade(this)
}

@JvmName("løsBistandExt")
fun Behandling.løsBistand(): Behandling {
    return testScenarioOrkestrator.løsBistand(this)
}

@JvmName("løsRefusjonskravExt")
fun Behandling.løsRefusjonskrav(): Behandling {
    return testScenarioOrkestrator.løsRefusjonskrav(this)
}

@JvmName("kvalitetssikreOkExt")
fun Behandling.kvalitetssikreOk(): Behandling {
    return testScenarioOrkestrator.kvalitetssikreOk(this)
}

@JvmName("løsBeregningstidspunktExt")
fun Behandling.løsBeregningstidspunkt(): Behandling {
    return testScenarioOrkestrator.løsBeregningstidspunkt(this)
}

@JvmName("løsManuellInntektVurderingExt")
fun Behandling.løsManuellInntektVurdering(): Behandling {
    return testScenarioOrkestrator.løsManuellInntektVurdering(this)
}

@JvmName("løsFastsettYrkesskadeInntektExt")
fun Behandling.løsFastsettYrkesskadeInntekt(): Behandling {
    return testScenarioOrkestrator.løsFastsettYrkesskadeInntekt(this)
}

@JvmName("løsBarnetilleggExt")
fun Behandling.løsBarnetillegg(): Behandling {
    return testScenarioOrkestrator.løsBarnetillegg(this)
}

@JvmName("løsForutgaaendeMedlemskapExt")
fun Behandling.løsForutgåendeMedlemskap(): Behandling {
    return testScenarioOrkestrator.løsForutgåendeMedlemskap(this)
}

@JvmName("løsSoningsforholdExt")
fun Behandling.løsSoningsforhold(): Behandling {
    return testScenarioOrkestrator.løsSoningsforhold(this)
}

@JvmName("fattVedtakExt")
fun Behandling.fattVedtakEllerSendRetur(returVed: Definisjon? = null): Behandling {
    return testScenarioOrkestrator.fattVedtakEllerSendRetur(this, returVed)
}


fun Behandling.løsUtenSamordning(): Behandling {
    return this.løsAvklaringsBehov(
        AvklarSamordningGraderingLøsning(
            vurderingerForSamordning = VurderingerForSamordning("", true, null, emptyList())
        )
    )
}

@JvmName("tjenestepensjonRefusjonskravVurderingExt")
fun Behandling.løsTjenestepensjonRefusjonskravVurdering(returVed: Definisjon? = null): Behandling {
    return testScenarioOrkestrator.løsTjenestepensjonRefusjonskravVurdering(this)
}

fun Behandling.løsSamordning(periode: Periode): Behandling {
    return this.løsAvklaringsBehov(
        AvklarSamordningGraderingLøsning(
            vurderingerForSamordning = VurderingerForSamordning(
                "samordning ok",
                true,
                null,
                listOf(SamordningVurderingData(Ytelse.OMSORGSPENGER, periode, gradering = 70))
            )
        )
    )
}

@JvmName("løsAvklaringsBehovExt")
fun Behandling.løsAvklaringsBehov(
    avklaringsBehovLøsning: AvklaringsbehovLøsning,
    bruker: Bruker = Bruker("SAKSBEHANDLER"),
    ingenEndringIGruppe: Boolean = false
): Behandling {
    return testScenarioOrkestrator.løsAvklaringsBehov(
        behandling = this,
        avklaringsBehovLøsning = avklaringsBehovLøsning,
        bruker = bruker,
        ingenEndringIGruppe = ingenEndringIGruppe
    )
}

private fun opprettTestKlage(testcaseDTO: OpprettTestcaseDTO) {
    val sak = sendInnSøknad(testcaseDTO)
    sendInnKlage(sak)
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