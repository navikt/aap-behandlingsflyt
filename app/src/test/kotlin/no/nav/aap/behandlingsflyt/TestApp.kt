package no.nav.aap.behandlingsflyt

import com.papsign.ktor.openapigen.route.apiRouting
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.response.respondWithStatus
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.TypeBrev
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.YtelseTypeCode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.gateway.SamhandlerForholdDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.gateway.SamhandlerYtelseDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.gateway.TjenestePensjonRespons
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.gateway.TpOrdning
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Institusjonstype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.Oppholdstype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.Uføre
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UføreSøknad
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Gjenopptak
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Klage
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravType
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.MuligRettFra
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.MuligRettFraÅrsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.NyttKrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Søknadsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.SøknadsdatoÅrsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Tilleggsopplysning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.TrukketSøknad
import no.nav.aap.behandlingsflyt.hendelse.avløp.BehandlingHendelseServiceFactory
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.ArbeidsevneNedsattValg
import no.nav.aap.behandlingsflyt.help.ident
import no.nav.aap.behandlingsflyt.integrasjon.institusjonsopphold.InstitusjonsoppholdJSON
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.AndreUtbetalingerDto
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.FastlegeDto
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.FastlegeKontaktInformasjonDto
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Ident
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.JaNei
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.JaNeiVetIkke
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
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate.SaksnummerParameter
import no.nav.aap.behandlingsflyt.test.FakeServers
import no.nav.aap.behandlingsflyt.test.FiktivtHelseoppholdNavnGenerator
import no.nav.aap.behandlingsflyt.test.JSONTestPersonService
import no.nav.aap.behandlingsflyt.test.TexasPortHolder
import no.nav.aap.behandlingsflyt.test.LokalUnleash
import no.nav.aap.behandlingsflyt.test.modell.TestPerson
import no.nav.aap.behandlingsflyt.test.modell.TestYrkesskade
import no.nav.aap.behandlingsflyt.test.testGatewayProvider
import no.nav.aap.dokumentinnhenting.kontrakt.BehandlerDto
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.testutil.ManuellMotorImpl
import no.nav.aap.verdityper.dokument.JournalpostId
import no.nav.aap.verdityper.dokument.Kanal
import org.slf4j.LoggerFactory
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy
import org.testcontainers.postgresql.PostgreSQLContainer
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.sql.DataSource
import kotlin.random.Random

private val log = LoggerFactory.getLogger("TestApp")
lateinit var testScenarioOrkestrator: TestScenarioOrkestrator
lateinit var motor: ManuellMotorImpl
lateinit var datasource: DataSource

data class IdentOgOpphold(val ident: String, val opphold: List<InstitusjonsoppholdJSON>)

// Kjøres opp for å få logback i console uten json
fun main() {
    val dbConfig = initDbConfig()

    TexasPortHolder.setPort(8081)
    FakeServers.start(JSONTestPersonService())

    // Starter server
    embeddedServer(Netty, configure = {
        shutdownTimeout = TimeUnit.SECONDS.toMillis(20)
        connector {
            port = 8080
        }
    }) {
        val gatewayProvider = testGatewayProvider(LokalUnleash::class) {
            register<BehandlingHendelseServiceFactory>()
        }
        val repositoryRegistry = postgresRepositoryRegistry

        // Useful for connecting to the test database locally
        // jdbc URL contains the host and port and database name.
        server(dbConfig, repositoryRegistry, gatewayProvider)

        datasource = initDatasource(dbConfig)
        motor = lazy {
            ManuellMotorImpl(
                datasource,
                jobber = ProsesseringsJobber.alle(),
                repositoryRegistry = repositoryRegistry,
                gatewayProvider = gatewayProvider
            )
        }.value

        testScenarioOrkestrator = TestScenarioOrkestrator(gatewayProvider, datasource, motor)

        apiRouting {
            route("/test") {
                route("/opprett") {
                    post<Unit, OpprettTestcaseDTO, OpprettTestcaseDTO> { _, dto ->
                        opprettNySakOgBehandling(dto, gatewayProvider, repositoryRegistry)
                        respond(dto)
                    }
                }

                route("/endre/{saksnummer}/legg-til-institusjonsopphold") {
                    post<SaksnummerParameter, Unit, LeggTilInstitusjonsoppholdDTO> { param, dto ->
                        val (ident, eksisterendeOpphold) = hentIdentOgOppholdForSak(
                            Saksnummer(param.saksnummer),
                            repositoryRegistry,
                            gatewayProvider
                        )

                        val fakePersoner = JSONTestPersonService()
                        val person = fakePersoner.hentPerson(ident)

                        if (person != null) {
                            val oppdaterteOpphold = slåSammenInstitusjonsopphold(eksisterendeOpphold, dto.opphold)
                            fakePersoner.oppdater(person.medInstitusjonsopphold(oppdaterteOpphold))
                            respondWithStatus(HttpStatusCode.OK)
                        } else {
                            log.warn("Finner ikke person med ident $ident for å legge til institusjonsopphold")
                            respondWithStatus(HttpStatusCode.BadRequest)
                        }
                    }
                }

                route("/endre/{saksnummer}/legg-til-yrkesskade") {
                    post<SaksnummerParameter, Unit, LeggTilYrkesskadeDTO> { param, dto ->
                        val ident = hentIdentForSak(Saksnummer(param.saksnummer))

                        val fakePersoner = JSONTestPersonService()
                        val nyeYrkesskader = dto.yrkesskader.mapNotNull { entry ->
                            when (entry.kilde) {
                                Kilde.SØKNAD -> null
                                Kilde.REGISTER -> TestYrkesskade(
                                    skadedato = entry.skadedato,
                                    skadeart = entry.skadeart,
                                    diagnose = entry.diagnose,
                                    skadebeskrivelse = entry.skadebeskrivelse,
                                    vedtaksdato = entry.vedtaksdato,
                                )
                            }
                        }.ifEmpty { listOf(TestYrkesskade()) }

                        val oppdatertPerson = fakePersoner.hentPerson(ident)?.let {
                            it.medYrkesskade(it.yrkesskade + nyeYrkesskader)
                        }

                        if (oppdatertPerson != null) {
                            fakePersoner.oppdater(oppdatertPerson)
                            respondWithStatus(HttpStatusCode.OK)
                        } else {
                            log.warn("Finner ikke person med ident $ident for å legge til yrkesskade")
                            respondWithStatus(HttpStatusCode.BadRequest)
                        }
                    }
                }

                route("/endre/{saksnummer}/legg-til-kravvurdering") {
                    post<SaksnummerParameter, Unit, LeggTilKravVurderingDTO> { param, dto ->
                        val behandling = hentSisteBehandlingForSak(
                            hentSakId(Saksnummer(param.saksnummer)),
                            gatewayProvider
                        )
                        datasource.transaction { connection ->
                            val repositoryProvider = postgresRepositoryRegistry.provider(connection)
                            val kravRepository = repositoryProvider.provide<KravRepository>()
                            val vurderinger = dto.kravVurderinger.map { krav ->
                                mapKravVurdering(krav, behandling.id)
                            }.toSet()
                            kravRepository.lagre(behandling.id, vurderinger)
                        }
                        respondWithStatus(HttpStatusCode.OK)
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

private fun hentIdentOgOppholdForSak(
    saksnummer: Saksnummer,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider
): IdentOgOpphold {
    return datasource.transaction(readOnly = true) { connection ->
        val repositoryProvider = repositoryRegistry.provider(connection)
        val sakRepository = repositoryProvider.provide<SakRepository>()
        val sak = sakRepository.hent(saksnummer)
        val ident = sak.person.aktivIdent().identifikator

        val sisteBehandlingId = hentSisteBehandlingForSak(sak.id, gatewayProvider)

        val oppholdFraDb = sisteBehandlingId
            .let { repositoryProvider.provide<InstitusjonsoppholdRepository>().hentHvisEksisterer(it.id) }
            ?.oppholdene
            ?.opphold
            ?.map { segment ->
                InstitusjonsoppholdJSON(
                    organisasjonsnummer = segment.verdi.orgnr,
                    kategori = segment.verdi.kategori.name,
                    institusjonstype = segment.verdi.type.name,
                    startdato = segment.periode.fom,
                    forventetSluttdato = segment.periode.tom,
                    institusjonsnavn = segment.verdi.navn
                )
            } ?: emptyList()

        IdentOgOpphold(ident, oppholdFraDb)
    }
}

private fun slåSammenInstitusjonsopphold(
    fraDb: List<InstitusjonsoppholdJSON>,
    fraFrontend: List<InstitusjonsoppholdItemDTO>
): List<InstitusjonsoppholdJSON> {
    val oppdaterte = fraFrontend.map { nytt ->
        val eksisterende = fraDb.find { it.startdato == nytt.oppholdFom }
        eksisterende?.let {
            if (it.forventetSluttdato != nytt.oppholdTom)
                it.copy(forventetSluttdato = nytt.oppholdTom)
            else it
        } ?: genererInstitusjonsopphold(nytt)
    }

    val ikkeOppdaterte = fraDb.filter { db ->
        fraFrontend.none { it.oppholdFom == db.startdato }
    }

    return ikkeOppdaterte + oppdaterte
}

private fun genererInstitusjonsopphold(oppholdDto: InstitusjonsoppholdItemDTO) =
    InstitusjonsoppholdJSON(
        organisasjonsnummer = Random.nextInt(911111111, 999999999).toString(),
        kategori = oppholdDto.oppholdstype.name,
        institusjonstype = oppholdDto.institusjonstype.name,
        forventetSluttdato = oppholdDto.oppholdTom,
        startdato = oppholdDto.oppholdFom,
        institusjonsnavn = FiktivtHelseoppholdNavnGenerator.generer()
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
    val ident = ident()

    return TestPerson(
        identer = setOf(ident),
        fødselsdato = Fødselsdato(dto.fodselsdato),
    )
}

private fun mapTilSøknad(dto: OpprettTestcaseDTO, urelaterteBarn: List<TestPerson>, fastlege: BehandlerDto?): SøknadV0 {
    val søknadStudentDto = if (dto.student) {
        SøknadStudentDto(
            erStudent = StudentStatus.Avbrutt,
            kommeTilbake = JaNeiVetIkke.Ja
        )
    } else {
        null
    }
    val harYrkesskadeFraSøknad = dto.yrkesskader.any { it.kilde == Kilde.SØKNAD && it.harYrkesskade }
    val harYrkesskade = if (harYrkesskadeFraSøknad) "JA" else "NEI"

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
        log.info("Oppretter ikke oppgitte barn siden det ikke er noen urelaterte barn i testcase")
        null
    }
    val harMedlemskap = if (dto.medlemskap) "JA" else "NEI"

    return SøknadV0(
        andreUtbetalinger = AndreUtbetalingerDto(
            lønn = dto.andreUtbetalinger?.lønn,
            stønad = dto.andreUtbetalinger?.stønad,
            afp = dto.andreUtbetalinger?.afp
        ),
        student = søknadStudentDto,
        yrkesskade = harYrkesskade,
        oppgitteBarn = oppgitteBarn,
        medlemskap = SøknadMedlemskapDto(harMedlemskap, null, null, null, emptyList()),
        fastlege = listOfNotNull(fastlegeISøknad(dto, fastlege)),
        andreBehandlere = andreBehandlereOppgittISøknad(dto)
    )
}

private fun fastlegeISøknad(
    dto: OpprettTestcaseDTO,
    fastlege: BehandlerDto?
): FastlegeDto? = if (dto.fastlege?.harFastlege ?: false) {
    if (!dto.fastlege.harEndretFastlege && fastlege == null) {
        null
    } else if (!dto.fastlege.harEndretFastlege && fastlege != null) {
        FastlegeDto(
            navn = listOfNotNull(fastlege.fornavn, fastlege.mellomnavn, fastlege.etternavn).joinToString { " " },
            behandlerRef = fastlege.behandlerRef,
            kontaktinformasjon = FastlegeKontaktInformasjonDto(
                kontor = fastlege.kontor,
                adresse = fastlege.adresse,
                telefon = fastlege.telefon
            ),
            erRegistrertFastlegeRiktig = if (dto.fastlege.varFastlegeRiktigPåSøknadstidspunkt) JaNei.Ja else JaNei.Nei,
        )
    } else {
        FastlegeDto(
            navn = "Navn",
            behandlerRef = UUID.randomUUID().toString(),
            kontaktinformasjon = FastlegeKontaktInformasjonDto(
                kontor = "kontor",
                adresse = "adresse",
                telefon = "telefon"
            ),
            erRegistrertFastlegeRiktig = if (dto.fastlege.varFastlegeRiktigPåSøknadstidspunkt) JaNei.Ja else JaNei.Nei,
        )
    }
} else {
    null
}

private fun andreBehandlereOppgittISøknad(dto: OpprettTestcaseDTO): List<no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.BehandlerDto> =
    if (dto.fastlege?.harOppgittAndreBehandlere ?: false) {
        List(Random.nextInt(1, 3)) { index ->
            no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.BehandlerDto(
                firstname = "firstname $index",
                lastname = "lastname $index",
                legekontor = "legekontor $index",
                id = "id $index",
                gateadresse = "gateadresse $index",
                postnummer = "postnummer $index",
                poststed = "poststed $index",
                telefon = "$index$index$index$index$index$index$index$index",
            )
        }
    } else {
        emptyList()
    }

private fun sendInnSøknad(
    dto: OpprettTestcaseDTO,
    gatewayProvider: GatewayProvider,
    repositoryRegistry: RepositoryRegistry
): Sak {
    val ident = ident()
    val barn = dto.barn.filter { it.harRelasjon }.map { genererBarn(it) }
    val urelaterteBarnIPDL = dto.barn.filter { !it.harRelasjon && it.skalFinnesIPDL }.map { genererBarn(it) }
    val urelaterteBarnIkkeIPDL = dto.barn.filter { !it.harRelasjon && !it.skalFinnesIPDL }.map { genererBarn(it) }
    val jsonTestPersonService = JSONTestPersonService()
    val fastlege = genererFastlege(dto)
    barn.forEach { jsonTestPersonService.leggTil(it) }
    urelaterteBarnIPDL.forEach { jsonTestPersonService.leggTil(it) }
    jsonTestPersonService.leggTil(
        TestPerson(
            identer = setOf(ident),
            fødselsdato = Fødselsdato(dto.fødselsdato),
            yrkesskade = dto.yrkesskader.mapNotNull { entry ->
                when (entry.kilde) {
                    Kilde.SØKNAD -> null
                    Kilde.REGISTER -> TestYrkesskade(
                        skadedato = entry.skadedato,
                        skadeart = entry.skadeart,
                        diagnose = entry.diagnose,
                        skadebeskrivelse = entry.skadebeskrivelse,
                        vedtaksdato = entry.vedtaksdato,
                    )
                }
            },
            uføre = dto.uføre?.let {
                Uføre(
                    virkningstidspunkt = dto.uføreTidspunkt!!,
                    uføregrad = Prosent(it),
                    uføregradTom = dto.uføregradTom,
                )
            },
            uføreSøknad = dto.uføreSøknadDato?.let {
                UføreSøknad(
                    soknadsdato = dto.uføreSøknadDato,
                    sakId = Random.nextLong(),

                    )
            },
            barn = barn,
            institusjonsopphold = listOfNotNull(
                if (dto.institusjoner.fengsel == true) genererFengselsopphold() else null,
                if (dto.institusjoner.sykehus == true) genererSykehusopphold() else null,
            ),
            inntekter = dto.inntekterPerAr.orEmpty().map { inn -> inn.to() },
            sykepenger = dto.sykepenger.map {
                TestPerson.Sykepenger(
                    grad = it.grad,
                    periode = it.periode
                )
            },
            dagpenger = dto.dagpenger.map {
                TestPerson.Dagpenger(
                    periode = it.periode,
                    kilde = it.kilde,
                    dagpengerYtelseType = it.dagpengerYtelseType
                )
            },
            tiltakspenger = dto.tiltakspenger.map {
                TestPerson.Tiltakspenger(
                    periode = it.periode,
                    kilde = it.kilde,
                    ytelseType = it.ytelseType
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
            fastlege = fastlege
        )
    )

    val sak = datasource.transaction { connection ->
        val repositoryProvider = repositoryRegistry.provider(connection)
        val sakService = PersonOgSakService(gatewayProvider, repositoryProvider)
        val sak = sakService.finnEllerOpprett(ident, dto.søknadsdato ?: LocalDate.now())

        val flytJobbRepository = FlytJobbRepository(connection)

        val melding = mapTilSøknad(dto, urelaterteBarnIkkeIPDL + urelaterteBarnIPDL, fastlege)

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

private fun genererFastlege(dto: OpprettTestcaseDTO): BehandlerDto? = if (dto.fastlege?.harFastlege ?: false) {
    BehandlerDto(
        behandlerRef = UUID.randomUUID().toString(),
        hprId = Random.nextInt(100000000, 999999999).toString(),
        fornavn = "Fornavn",
        mellomnavn = "Mellomnavn",
        etternavn = "Etternavn",
        kontor = "Kontor",
        adresse = "Adresse",
        postnummer = "0000",
        poststed = "Poststed",
        telefon = "00000000",
    )
} else {
    null
}

private fun opprettNySakOgBehandling(
    dto: OpprettTestcaseDTO,
    gatewayProvider: GatewayProvider,
    repositoryRegistry: RepositoryRegistry
): Sak {
    val sak = sendInnSøknad(dto, gatewayProvider, repositoryRegistry)

    if (dto.steg in listOf(StegType.START_BEHANDLING, StegType.AVKLAR_STUDENT)) return sak

    motor.kjørJobber()

    // fullfør førstegangsbehandling
    log.info("Fullfører førstegangsbehandling for sak ${sak.id}")
    val behandling = hentSisteBehandlingForSak(sak.id, gatewayProvider)

    with(testScenarioOrkestrator) {
        // Student eller sykdom
        if (dto.student) {
            løsStudent(behandling, vurderingenGjelderFra = dto.søknadsdato ?: sak.rettighetsperiode.fom)
        } else {
            if (dto.steg == StegType.AVKLAR_SYKDOM) return sak
            løsSykdom(
                behandling = behandling,
                vurderingGjelderFra = dto.søknadsdato ?: sak.rettighetsperiode.fom,
                harNedsattArbeidsevne = if (dto.harNedsattArbeidsevne) ArbeidsevneNedsattValg.JA else ArbeidsevneNedsattValg.NEI,
                erNedsettelseIArbeidsevneMerEnnHalvparten = dto.erNedsettelseIArbeidsevneMerEnnHalvparten
            )
        }

        val harBehandlingsgrunnlag = dto.harNedsattArbeidsevne && dto.erNedsettelseIArbeidsevneMerEnnHalvparten

        if (harBehandlingsgrunnlag) {
            if (dto.steg == StegType.VURDER_BISTANDSBEHOV) return sak
            løsBistand(behandling, dto.søknadsdato ?: sak.rettighetsperiode.fom)

            // Vurderinger i sykdom
            if (dto.steg == StegType.REFUSJON_KRAV) return sak
            løsRefusjonskrav(behandling)
        }

        if (dto.steg == StegType.SYKDOMSVURDERING_BREV) return sak
        else if (!dto.student) løsSykdomsvurderingBrev(behandling)

        if (dto.steg == StegType.BEKREFT_VURDERINGER_OPPFØLGING) return sak
        løsVurderingerOppfølgning(behandling)

        if (dto.steg == StegType.KVALITETSSIKRING) return sak
        kvalitetssikreOk(behandling)

        if (harBehandlingsgrunnlag) {
            // Yrkesskade
            val harYrkesskade = dto.yrkesskader.any { it.kilde == Kilde.REGISTER }
            if (harYrkesskade) {
                if (dto.steg == StegType.VURDER_YRKESSKADE) return sak
                løsYrkesSkade(behandling)
            }

            // Inntekt
            if (dto.steg == StegType.FASTSETT_BEREGNINGSTIDSPUNKT) return sak
            løsBeregningstidspunkt(behandling)

            if (harYrkesskade) {
                løsFastsettYrkesskadeInntekt(behandling)
            }

            val manglendeInntektsÅr = manglendeInntektsår(dto)
            if (manglendeInntektsÅr.isNotEmpty()) {
                if (dto.steg == StegType.MANGLENDE_LIGNING) return sak
                løsManuellInntektVurdering(behandling, manglendeInntektsÅr)
            }

            // Forutgående medlemskap
            if (!harYrkesskade) {
                if ((dto.steg == StegType.VURDER_MEDLEMSKAP)) return sak
                løsForutgåendeMedlemskap(behandling, sak)
            }

            // Oppholdskrav
            if (dto.steg == StegType.VURDER_OPPHOLDSKRAV) return sak
            løsOppholdskrav(behandling, sak)

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

private fun manglendeInntektsår(dto: OpprettTestcaseDTO): List<Int> {
    val søknadsdato = dto.søknadsdato ?: LocalDate.now()
    val nåværendeÅr = søknadsdato.year
    val siste3År = listOf(nåværendeÅr - 1, nåværendeÅr - 2, nåværendeÅr - 3)

    val registrerteÅr = dto.inntekterPerAr?.map { it.år }?.toSet() ?: emptySet()

    return siste3År.filter { år -> år !in registrerteÅr }
}

private fun hentIdentForSak(saksnummer: Saksnummer): String {
    return datasource.transaction { connection ->
        val repositoryProvider = postgresRepositoryRegistry.provider(connection)
        val sakRepository = repositoryProvider.provide<SakRepository>()
        val sak = sakRepository.hent(saksnummer)

        sak.person.aktivIdent().identifikator
    }
}

private fun hentSisteBehandlingForSak(sakId: SakId, gatewayProvider: GatewayProvider): Behandling {
    return datasource.transaction { connection ->
        val repositoryProvider = postgresRepositoryRegistry.provider(connection)
        val sbService = BehandlingService(
            repositoryProvider,
            gatewayProvider
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

private fun hentSakId(saksnummer: Saksnummer): SakId {
    return datasource.transaction(readOnly = true) { connection ->
        val repositoryProvider = postgresRepositoryRegistry.provider(connection)
        repositoryProvider.provide<SakRepository>().hent(saksnummer).id
    }
}

private fun mapKravVurdering(krav: KravVurderingTestDto, behandlingId: BehandlingId): KravVurdering {
    val journalpostId = JournalpostId("test-krav-${UUID.randomUUID()}")
    val now = Instant.now()
    return when (krav.kravType) {
        KravType.NYTT_KRAV_AAP -> NyttKrav(
            journalpostId = journalpostId,
            vurdertAv = "TESTBRUKER",
            begrunnelse = "Nytt krav: søknad mottatt i tide, trenger manuell vurdering av vilkår.",
            vurdertIBehandling = behandlingId,
            opprettet = now,
            søknadsdato = Søknadsdato(
                dato = krav.søknadsdato ?: LocalDate.now().minusMonths(3),
                årsak = SøknadsdatoÅrsak.SøknadMottatt
            ),
            muligRettFra = krav.muligRettFra?.let { MuligRettFra(it, MuligRettFraÅrsak.IkkeIStandTilÅSøkeTidligere) },
            kravdato = krav.kravdato ?: LocalDate.now().minusMonths(3)
        )
        KravType.GJENOPPTAK -> Gjenopptak(
            journalpostId = journalpostId,
            vurdertAv = "TESTBRUKER",
            begrunnelse = "Gjenopptak: nye opplysninger tilsier at saken skal vurderes på nytt.",
            vurdertIBehandling = behandlingId,
            opprettet = now,
            søknadsdato = Søknadsdato(
                dato = krav.søknadsdato ?: LocalDate.now().minusMonths(3),
                årsak = SøknadsdatoÅrsak.SøknadMottatt
            ),
            muligRettFra = krav.muligRettFra?.let { MuligRettFra(it, MuligRettFraÅrsak.IkkeIStandTilÅSøkeTidligere) },
            kravdato = krav.kravdato ?: LocalDate.now().minusMonths(3)
        )
        KravType.TRUKKET_SØKNAD -> TrukketSøknad(
            journalpostId = journalpostId,
            vurdertAv = "TESTBRUKER",
            begrunnelse = "Trukket søknad: søker har bekreftet at kravet ikke lenger opprettholdes.",
            vurdertIBehandling = behandlingId,
            opprettet = now,
        )
        KravType.KLAGE -> Klage(
            journalpostId = journalpostId,
            vurdertAv = "TESTBRUKER",
            begrunnelse = "Klage: tidligere vurdering bestrides, sendes til ny behandling.",
            vurdertIBehandling = behandlingId,
            opprettet = now,
        )
        KravType.TILLEGGSOPPLYSNING -> Tilleggsopplysning(
            journalpostId = journalpostId,
            vurdertAv = "TESTBRUKER",
            begrunnelse = "Tilleggsopplysning: mottatt ny dokumentasjon som påvirker rettighetsvurderingen.",
            vurdertIBehandling = behandlingId,
            opprettet = now,
        )
    }
}
