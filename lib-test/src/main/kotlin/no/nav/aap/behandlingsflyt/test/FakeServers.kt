package no.nav.aap.behandlingsflyt.test

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.runBlocking
import no.nav.aap.behandlingsflyt.behandling.dokumentinnhenting.BestillLegeerklæringDto
import no.nav.aap.behandlingsflyt.behandling.dokumentinnhenting.ForhåndsvisBrevRequest
import no.nav.aap.behandlingsflyt.behandling.dokumentinnhenting.HentStatusLegeerklæring
import no.nav.aap.behandlingsflyt.behandling.dokumentinnhenting.PurringLegeerklæringRequest
import no.nav.aap.behandlingsflyt.datadeling.sam.HentSamIdResponse
import no.nav.aap.behandlingsflyt.datadeling.sam.SamordneVedtakRequest
import no.nav.aap.behandlingsflyt.datadeling.sam.SamordneVedtakRespons
import no.nav.aap.behandlingsflyt.datadeling.sam.SamordningsmeldingApi
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.gateway.TjenestePensjonRespons
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.Anvist
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.ForeldrepengerRequest
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.ForeldrepengerResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.SykepengerRequest
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.SykepengerResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.Utbetalingsgrad
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.UtbetaltePerioder
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.Ytelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.Ytelser
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.dokumentinnhenting.LegeerklæringStatusResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.dokumentinnhenting.MeldingStatusType
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.adapter.InntektRequest
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.adapter.InntektResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.adapter.SumPi
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.adapter.MedlemskapResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.adapter.PERSON_QUERY
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.adapter.PERSON_QUERY_HISTORIKK
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.adapter.YrkesskadeModell
import no.nav.aap.behandlingsflyt.integrasjon.barn.BARN_RELASJON_QUERY
import no.nav.aap.behandlingsflyt.integrasjon.barn.PERSON_BOLK_QUERY
import no.nav.aap.behandlingsflyt.integrasjon.ident.IDENT_QUERY
import no.nav.aap.behandlingsflyt.integrasjon.ident.PdlPersoninfoGateway
import no.nav.aap.behandlingsflyt.integrasjon.organisasjon.NomData
import no.nav.aap.behandlingsflyt.integrasjon.organisasjon.NomDataRessurs
import no.nav.aap.behandlingsflyt.integrasjon.organisasjon.NorgEnhet
import no.nav.aap.behandlingsflyt.integrasjon.organisasjon.OrgEnhet
import no.nav.aap.behandlingsflyt.integrasjon.organisasjon.OrgTilknytning
import no.nav.aap.behandlingsflyt.integrasjon.ufore.UføreHistorikkRespons
import no.nav.aap.behandlingsflyt.integrasjon.ufore.UførePeriode
import no.nav.aap.behandlingsflyt.integrasjon.ufore.UføreRequest
import no.nav.aap.behandlingsflyt.integrasjon.ufore.UføreRespons
import no.nav.aap.behandlingsflyt.integrasjon.util.GraphQLResponse
import no.nav.aap.behandlingsflyt.integrasjon.yrkesskade.YrkesskadeRequest
import no.nav.aap.behandlingsflyt.integrasjon.yrkesskade.Yrkesskader
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.BehandlingFlytStoppetHendelse
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.StoppetBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.HentPerson
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.HentPersonBolkResult
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.PDLDødsfall
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.PdlFoedsel
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.PdlFolkeregisterPersonStatus
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.PdlGruppe
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.PdlIdent
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.PdlIdenter
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.PdlIdenterData
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.PdlIdenterDataResponse
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.PdlNavn
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.PdlNavnData
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.PdlNavnDataBolk
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.PdlPersonBolk
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.PdlPersonNavnDataResponse
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.PdlPersoninfo
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.PdlPersoninfoData
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.PdlPersoninfoDataResponse
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.PdlRelasjon
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.PdlRelasjonData
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.PdlRelasjonDataResponse
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.PdlRequest
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.PdlStatsborgerskap
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.PersonStatus
import no.nav.aap.behandlingsflyt.test.modell.MockUnleashFeature
import no.nav.aap.behandlingsflyt.test.modell.MockUnleashFeatures
import no.nav.aap.behandlingsflyt.test.modell.TestPerson
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.brev.kontrakt.BestillBrevResponse
import no.nav.aap.brev.kontrakt.BestillBrevV2Request
import no.nav.aap.brev.kontrakt.Brev
import no.nav.aap.brev.kontrakt.BrevbestillingResponse
import no.nav.aap.brev.kontrakt.Brevtype
import no.nav.aap.brev.kontrakt.FerdigstillBrevRequest
import no.nav.aap.brev.kontrakt.HentSignaturerRequest
import no.nav.aap.brev.kontrakt.HentSignaturerResponse
import no.nav.aap.brev.kontrakt.Innhold
import no.nav.aap.brev.kontrakt.Signatur
import no.nav.aap.brev.kontrakt.Språk
import no.nav.aap.brev.kontrakt.Status
import no.nav.aap.brev.kontrakt.Tekstbolk
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.meldekort.kontrakt.sak.MeldeperioderV0
import no.nav.aap.tilgang.BehandlingTilgangRequest
import no.nav.aap.tilgang.JournalpostTilgangRequest
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.SakTilgangRequest
import no.nav.aap.tilgang.TilgangResponse
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger


object FakeServers : AutoCloseable {
    private val log = LoggerFactory.getLogger(javaClass)
    private val azure = embeddedServer(Netty, port = AzurePortHolder.getPort(), module = { azureFake() })
    private val brev = embeddedServer(Netty, port = 0, module = { brevFake() })
    private val pdl = embeddedServer(Netty, port = 0, module = { pdlFake() })
    private val yrkesskade = embeddedServer(Netty, port = 0, module = { yrkesskadeFake() })
    private val inntekt = embeddedServer(Netty, port = 0, module = { poppFake() })
    private val oppgavestyring = embeddedServer(Netty, port = 0, module = { oppgavestyringFake() })
    private val saf = embeddedServer(Netty, port = 0, module = { safFake() })
    private val inst2 = embeddedServer(Netty, port = 0, module = { inst2Fake() })
    private val medl = embeddedServer(Netty, port = 0, module = { medlFake() })
    private val pesysFake = embeddedServer(Netty, port = 0, module = { pesysFake() })
    private val tilgang = embeddedServer(Netty, port = 0, module = { tilgangFake() })
    private val foreldrepenger = embeddedServer(Netty, port = 0, module = { fpFake() })
    private val sykepenger = embeddedServer(Netty, port = 0, module = { spFake() })
    private val statistikk = embeddedServer(Netty, port = 0, module = { statistikkFake() })
    private val dokumentinnhenting = embeddedServer(Netty, port = 0, module = { dokumentinnhentingFake() })
    private val ainntekt = embeddedServer(Netty, port = 0, module = { ainntektFake() })
    private val aareg = embeddedServer(Netty, port = 0, module = { aaregFake() })
    private val datadeling = embeddedServer(Netty, port = 0, module = { datadelingFake() })
    private val utbetal = embeddedServer(Netty, port = 0, module = { utbetalFake() })
    private val meldekort = embeddedServer(Netty, port = 0, module = { meldekortFake() })
    private val tjenestePensjon = embeddedServer(Netty, port = 0, module = { tjenestePensjonFake() })
    private val unleash = embeddedServer(Netty, port = 0, module = { unleashFake() })
    private val norg = embeddedServer(Netty, port = 0, module = { norgFake() })
    private val nom = embeddedServer(Netty, port = 0, module = { nomFake() })
    private val kabal = embeddedServer(Netty, port = 0, module = { kabalFake() })
    private val ereg = embeddedServer(Netty, port = 0, module = { eregFake() })
    private val sam = embeddedServer(Netty, port = 0, module = { sam() })

    internal val statistikkHendelser = mutableListOf<StoppetBehandling>()
    internal val legeerklæringStatuser = mutableListOf<LegeerklæringStatusResponse>()

    private val started = AtomicBoolean(false)

    private fun Application.oppgavestyringFake() {
        install(ContentNegotiation) {
            jackson {
                registerModule(JavaTimeModule())
            }
        }
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                this@oppgavestyringFake.log.info(
                    "Inntekt :: Ukjent feil ved kall til '{}'",
                    call.request.local.uri,
                    cause
                )
                call.respond(
                    status = HttpStatusCode.InternalServerError,
                    message = ErrorRespons(cause.message)
                )
            }
        }
        routing {
            post("/oppdater-oppgaver") {
                val received = call.receive<BehandlingFlytStoppetHendelse>()
                val åpneBehov = received.avklaringsbehov.filter { it.status.erÅpent() }
                    .map { Pair(it.avklaringsbehovDefinisjon.name, it.status) }
                FakeServers.log.info("Åpne behov $åpneBehov")
                FakeServers.log.info("Fikk oppgave-oppdatering: {}", received)
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }

    private fun Application.pesysFake() {
        install(ContentNegotiation) {
            jackson {
                registerModule(JavaTimeModule())
            }
        }
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                this@pesysFake.log.info("Inntekt :: Ukjent feil ved kall til '{}'", call.request.local.uri, cause)
                call.respond(
                    status = HttpStatusCode.InternalServerError,
                    message = ErrorRespons(cause.message)
                )
            }
        }
        routing() {
            get("/pen/api/uforetrygd/uforegrad") {
                val ident = requireNotNull(call.request.header("fnr"))
                val hentPerson = FakePersoner.hentPerson(ident)
                if (hentPerson == null) {
                    call.respond(HttpStatusCode.NotFound, "Fant ikke person med fnr $ident")
                    return@get
                }
                val uføregrad = hentPerson.uføre?.prosentverdi()
                if (uføregrad == null) {
                    call.respond(HttpStatusCode.NotFound)
                } else {
                    call.respond(HttpStatusCode.OK, UføreRespons(uforegrad = uføregrad))
                }
            }
            post("/pen/api/uforetrygd/uforehistorikk/perioder") {
                val body = call.receive<UføreRequest>()
                val hentPerson = FakePersoner.hentPerson(body.fnr)
                if (hentPerson == null) {
                    call.respond(HttpStatusCode.NotFound, "Fant ikke person med fnr ${body.fnr}")
                    return@post
                }
                val uføregrad = hentPerson.uføre?.prosentverdi()
                if (uføregrad == null) {
                    call.respond(HttpStatusCode.NotFound)
                } else {
                    call.respond(
                        HttpStatusCode.OK, UføreHistorikkRespons(
                            uforeperioder = listOf(
                                UførePeriode(
                                    uforegrad = uføregrad,
                                    uforegradTom = null,
                                    uforegradFom = null,
                                    uforetidspunkt = null,
                                    virkningstidspunkt = LocalDate.parse(body.dato)
                                )
                            )
                        )
                    )
                }
            }

        }
    }

    private fun Application.sam() {
        install(ContentNegotiation) {
            jackson {
                registerModule(JavaTimeModule())
                disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            }
        }

        install(StatusPages) {
            exception<Throwable> { call, cause ->
                this@sam.log.info("Inntekt :: Ukjent feil ved kall til '{}'", call.request.local.uri, cause)
                call.respond(
                    status = HttpStatusCode.InternalServerError,
                    message = ErrorRespons(cause.message)
                )
            }
        }

        routing {
            route("/api/vedtak") {
                route("samordne") {
                    post {
                        val req = call.receive<SamordneVedtakRequest>()

                        call.respond(
                            SamordneVedtakRespons(
                                ventPaaSvar = false
                            )
                        )
                    }
                }
                get {
                    val params = call.queryParameters
                    call.respond(
                        HttpStatusCode.OK, listOf(
                            HentSamIdResponse(
                                samordningsmeldinger = listOf(
                                    SamordningsmeldingApi(
                                        samId = 123L
                                    )
                                )
                            )
                        )
                    )
                }
            }
        }
    }

    private fun Application.poppFake() {
        install(ContentNegotiation) {
            jackson()
        }
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                this@poppFake.log.info("Inntekt :: Ukjent feil ved kall til '{}'", call.request.local.uri, cause)
                call.respond(
                    status = HttpStatusCode.InternalServerError,
                    message = ErrorRespons(cause.message)
                )
            }
        }
        routing {
            post {
                val req = call.receive<InntektRequest>()
                val person = hentEllerGenererTestPerson(req.fnr)

                for (år in req.fomAr..req.tomAr) {
                    //person.leggTilInntektHvisÅrMangler(Year.of(år), Beløp("0")) //TODO: Fjern denne helt
                }

                call.respond(
                    InntektResponse(person.inntekter().map { inntekt ->
                        SumPi(
                            inntektAr = inntekt.år.value,
                            belop = inntekt.beløp.verdi().toLong(),
                            inntektType = "Lønnsinntekt"
                        )
                    }.toList())
                )
            }
        }
    }

    private fun Application.pdlFake() {
        install(ContentNegotiation) {
            jackson {
                registerModule(JavaTimeModule())
            }
        }
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                this@pdlFake.log.info("PDL :: Ukjent feil ved kall til '{}'", call.request.local.uri, cause)
                call.respond(
                    status = HttpStatusCode.InternalServerError,
                    message = ErrorRespons(cause.message)
                )
            }
        }
        routing {
            post {
                val req = call.receive<PdlRequest>()

                when (req.query) {
                    IDENT_QUERY -> call.respond(identer(req))
                    PERSON_QUERY -> call.respond(personopplysninger(req))
                    PERSON_QUERY_HISTORIKK -> call.respond(personopplysningerHistorikk(req))
                    PdlPersoninfoGateway.PERSONINFO_QUERY -> call.respond(navn(req))
                    PdlPersoninfoGateway.PERSONINFO_BOLK_QUERY -> call.respond(bolknavn(req))
                    BARN_RELASJON_QUERY -> call.respond(barnRelasjoner(req))
                    PERSON_BOLK_QUERY -> call.respond(barn(req))
                    else -> call.respond(HttpStatusCode.BadRequest)
                }
            }
        }
    }

    private fun Application.tjenestePensjonFake() {
        install(ContentNegotiation) {
            jackson {
                registerModule(JavaTimeModule())
            }
        }
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                this@tjenestePensjonFake.log.info("TP :: Ukjent feil ved kall til '{}'", call.request.local.uri, cause)
                call.respond(
                    status = HttpStatusCode.InternalServerError,
                    message = ErrorRespons(cause.message)
                )
            }
        }
        //create route
        routing {
            get("/api/tjenestepensjon/getActiveForholdMedActiveYtelser") {
                val fomDate = call.request.queryParameters["fomDate"]
                val tomDate = call.request.queryParameters["tomDate"]
                val ident = call.request.headers["fnr"] ?: ""
                val fakePerson = FakePersoner.hentPerson(ident)

                if (fakePerson != null && fakePerson.tjenestePensjon != null) {
                    call.respond(fakePerson.tjenestePensjon)
                } else {
                    call.respond(TjenestePensjonRespons(ident))
                }
            }
        }
    }


    private fun Application.unleashFake() {
        install(ContentNegotiation) {
            register(
                ContentType.Application.Json,
                JacksonConverter(objectMapper = DefaultJsonMapper.objectMapper(), true)
            )
        }
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                this@unleashFake.log.info("Unleash :: Ukjent feil ved kall til '{}'", call.request.local.uri, cause)
                call.respond(
                    status = HttpStatusCode.InternalServerError,
                    message = ErrorRespons(cause.message)
                )
            }
        }
        //create route
        routing {
            get("/api/client/features") {
                val features = BehandlingsflytFeature.entries.map { MockUnleashFeature(it.name, true) }
                val response = MockUnleashFeatures(features = features)

                call.respond(response)
            }
        }
    }

    private fun Application.tilgangFake() {
        install(ContentNegotiation) {
            jackson()
        }
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                this@tilgangFake.log.info(
                    "TILGANG :: Ukjent feil ved kall til '{}'",
                    call.request.local.uri,
                    cause
                )
                call.respond(
                    status = HttpStatusCode.InternalServerError,
                    message = ErrorRespons(cause.message)
                )
            }
        }
        routing {
            post("/tilgang/sak") {
                call.receive<SakTilgangRequest>()
                call.respond(TilgangResponse(true))
            }
        }
        routing {
            post("/tilgang/behandling") {
                call.receive<BehandlingTilgangRequest>()
                call.respond(
                    TilgangResponse(
                        true,
                        tilgangIKontekst = mapOf(Operasjon.SAKSBEHANDLE to true)
                    ))
            }
        }
        routing {
            post("/tilgang/journalpost") {
                call.receive<JournalpostTilgangRequest>()
                call.respond(TilgangResponse(true))
            }
        }
    }

    private fun Application.dokumentinnhentingFake() {
        install(ContentNegotiation) {
            jackson {
                registerModule(JavaTimeModule())
            }
        }
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                this@dokumentinnhentingFake.log.info(
                    "DOKUMENTINNHENTING :: Ukjent feil ved kall til '{}'",
                    call.request.local.uri,
                    cause
                )
                call.respond(
                    status = HttpStatusCode.InternalServerError,
                    message = ErrorRespons(cause.message)
                )
            }
        }
        routing {
            post("/api/dokumentinnhenting/syfo/bestill") {
                val dto = call.receive<BestillLegeerklæringDto>()
                val dialogmeldingId = UUID.randomUUID()
                legeerklæringStatuser.add(
                    LegeerklæringStatusResponse(
                        dialogmeldingId,
                        MeldingStatusType.OK,
                        "",
                        dto.behandlerRef,
                        dto.behandlerNavn,
                        UUID.randomUUID().toString(),
                        dto.saksnummer,
                        LocalDateTime.now(),
                        dto.fritekst
                    )
                )
                call.respond(dialogmeldingId.toString())
            }
            post("/api/dokumentinnhenting/syfo/purring") {
                call.receive<PurringLegeerklæringRequest>()
                val dialogmeldingId = UUID.randomUUID()
                legeerklæringStatuser.add(
                    LegeerklæringStatusResponse(
                        dialogmeldingId,
                        MeldingStatusType.OK,
                        "",
                        "behandlerRef",
                        "behandlerNavn",
                        UUID.randomUUID().toString(),
                        "saksnummer",
                        LocalDateTime.now(),
                        "fritekst"
                    )
                )
                call.respond(dialogmeldingId.toString())
            }
        }
        routing {
            get("/api/dokumentinnhenting/syfo/status/{saksnummer}") {
                val req = call.receive<HentStatusLegeerklæring>()
                val statuser = legeerklæringStatuser.filter { it.saksnummer == req.saksnummer }
                call.respond(statuser)
            }
        }
        routing {
            post("/api/dokumentinnhenting/syfo/brevpreview") {
                val req = call.receive<ForhåndsvisBrevRequest>()
                val fnr = 12341234123
                val navn = "Ronny Råkjører"

                val brev = """
                    Forespørsel om legeerklæring ved arbeidsuførhet\n
                    Gjelder pasient: $navn., $fnr.\n
                    Nav trenger opplysninger fra deg vedrørende din pasient. Du kan utelate opplysninger som etter din vurdering faller utenfor formålet.\n
                    «Legeerklæring ved arbeidsuførhet» leveres på blankett Nav 08-07.08, og honoreres med takst L40.\n
                    ${req.fritekst}\n
                    Lovhjemmel\n
                    Folketrygdloven § 21-4 andre ledd gir Nav rett til å innhente nødvendige opplysninger. Dette gjelder selv om opplysningene er taushetsbelagte, jf. § 21-4 sjette ledd.\n
                    Pålegget om utlevering av opplysninger kan påklages etter forvaltningsloven § 14.\n
                    Klageadgangen gjelder kun lovligheten i pålegget. Fristen for å klage er tre dager etter at pålegget er mottatt. Klagen kan fremsettes muntlig eller skriftlig.\n
                """.trimIndent()

                call.respond(brev)
            }
        }
    }

    private fun Application.statistikkFake() {
        install(ContentNegotiation) {
            jackson {
                registerModule(JavaTimeModule())
            }
        }
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                this@statistikkFake.log.info(
                    "STATISTIKK :: Ukjent feil ved kall til '{}'",
                    call.request.local.uri,
                    cause
                )
                call.respond(
                    status = HttpStatusCode.InternalServerError,
                    message = ErrorRespons(cause.message)
                )
            }
        }
        routing {
            post("/stoppetBehandling") {
                val receive = call.receive<StoppetBehandling>()
                statistikkHendelser.add(receive)
                call.respond(status = HttpStatusCode.Accepted, message = "{}")
            }
        }
    }

    private fun Application.fpFake() {
        install(ContentNegotiation) {
            jackson {
                registerModule(JavaTimeModule())
                disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            }
        }
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                this@fpFake.log.info(
                    "FORELDREPENGER :: Ukjent feil ved kall til '{}'",
                    call.request.local.uri,
                    cause
                )
                call.respond(
                    status = HttpStatusCode.InternalServerError,
                    message = ErrorRespons(cause.message)
                )
            }
        }
        routing {
            post("/hent-ytelse-vedtak") {
                val req = call.receive<ForeldrepengerRequest>()
                val ident = req.ident.verdi
                val fakePerson = FakePersoner.hentPerson(ident)
                if (fakePerson?.foreldrepenger != null) {
                    val foreldrepenger = fakePerson.foreldrepenger

                    // TODO:  utvid til å støtte andre ytelser
                    val response = ForeldrepengerResponse(
                        ytelser = listOf(
                            Ytelse(
                                ytelse = Ytelser.FORELDREPENGER,
                                saksnummer = 352017890,
                                kildesystem = "FPSAK",
                                ytelseStatus = "UNDER_BEHANDLING", // burde kanskje være LØPENDE?
                                vedtattTidspunkt = LocalDate.now().minusMonths(2),
                                anvist = foreldrepenger.map {
                                    Anvist(
                                        periode = it.periode,
                                        utbetalingsgrad = Utbetalingsgrad(it.grad),
                                        beløp = null,
                                    )
                                }
                            )
                        ))

                    @Language("JSON")
                    val foreldrepengerOgSvangerskapspengerResponse = """
           [{
            "aktor": {
                "verdi": $ident
            },
            "vedtattTidspunkt": "2024-08-22T14:52:09.94",
            "ytelse": "FORELDREPENGER",
            "saksnummer": 352017890,
            "vedtakReferanse": "678d51f5-fe1e-4b45-be04-53d3c0ab9b17",
            "ytelseStatus": "UNDER_BEHANDLING",
            "kildesystem": "FPSAK",
            "periode": {
                "fom": "2024-09-05",
                "tom": "2025-08-13"
            },
            "tilleggsopplysninger": null,
            "anvist": [
                {
                    "periode": {
                        "fom": "2024-11-07",
                        "tom": "2025-08-13"
                    },
                    "beløp": null,
                    "dagsats": {
                        "verdi": 1419.0
                    },
                    "utbetalingsgrad": {
                        "verdi": 100.0
                    },
                    "andeler": [
                        {
                            "arbeidsgiverIdent": {
                                "ident": 896929119
                            },
                            "dagsats": {
                                "verdi": 1419.0
                            },
                            "utbetalingsgrad": {
                                "verdi": 100.0
                            },
                            "refusjonsgrad": {
                                "verdi": 0.0
                            },
                            "inntektklasse": "ARBEIDSTAKER"
                        }
                    ]
                },
                {
                    "periode": {
                        "fom": "2024-09-05",
                        "tom": "2024-09-25"
                    },
                    "beløp": null,
                    "dagsats": {
                        "verdi": 1419.0
                    },
                    "utbetalingsgrad": {
                        "verdi": 100.0
                    },
                    "andeler": [
                        {
                            "arbeidsgiverIdent": {
                                "ident": 896929119
                            },
                            "dagsats": {
                                "verdi": 1419.0
                            },
                            "utbetalingsgrad": {
                                "verdi": 100.0
                            },
                            "refusjonsgrad": {
                                "verdi": 0.0
                            },
                            "inntektklasse": "ARBEIDSTAKER"
                        }
                    ]
                },
                {
                    "periode": {
                        "fom": "2024-09-26",
                        "tom": "2024-11-06"
                    },
                    "beløp": null,
                    "dagsats": {
                        "verdi": 1419.0
                    },
                    "utbetalingsgrad": {
                        "verdi": 100.0
                    },
                    "andeler": [
                        {
                            "arbeidsgiverIdent": {
                                "ident": 896929119
                            },
                            "dagsats": {
                                "verdi": 1419.0
                            },
                            "utbetalingsgrad": {
                                "verdi": 100.0
                            },
                            "refusjonsgrad": {
                                "verdi": 0.0
                            },
                            "inntektklasse": "ARBEIDSTAKER"
                        }
                    ]
                }
            ]
        }]
        """
                    call.respond(response.ytelser)

                } else {
                    call.respond(emptyList<Ytelse>())
                }
            }
        }
    }

    private fun Application.spFake() {
        install(ContentNegotiation) {
            jackson {
                registerModule(JavaTimeModule())
                disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            }
        }
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                this@spFake.log.info(
                    "SYKEPENGER :: Ukjent feil ved kall til '{}'",
                    call.request.local.uri,
                    cause
                )
                call.respond(
                    status = HttpStatusCode.InternalServerError,
                    message = ErrorRespons(cause.message)
                )
            }
        }
        routing {
            post("/utbetalte-perioder-aap") {
                val request = call.receive<SykepengerRequest>()
                val fakePerson = FakePersoner.hentPerson(request.personidentifikatorer.first())
                if (fakePerson?.sykepenger != null) {
                    call.respond(
                        SykepengerResponse(
                            utbetaltePerioder = fakePerson.sykepenger().map {
                                UtbetaltePerioder(
                                    fom = it.periode.fom,
                                    tom = it.periode.tom,
                                    grad = it.grad
                                )
                            }
                        ))
                } else {
                    call.respond(SykepengerResponse(emptyList()))
                }
            }
        }
    }

    private fun Application.aaregFake() {
        @Language("JSON")
        val aaregResponse = """
            {
              "arbeidsforholdoversikter": [
                {
                  "type": {
                    "kode": "ordinaertArbeidsforhold",
                    "beskrivelse": "Ordinært arbeidsforhold"
                  },
                  "arbeidstaker": {
                    "identer": [
                      {
                        "type": "AKTORID",
                        "ident": "2336200023418",
                        "gjeldende": true
                      },
                      {
                        "type": "FOLKEREGISTERIDENT",
                        "ident": "244991*****",
                        "gjeldende": true
                      }
                    ]
                  },
                  "arbeidssted": {
                    "type": "Underenhet",
                    "identer": [
                      {
                        "type": "ORGANISASJONSNUMMER",
                        "ident": "896929119",
                        "gjeldende": null
                      }
                    ]
                  },
                  "opplysningspliktig": {
                    "type": "Hovedenhet",
                    "identer": [
                      {
                        "type": "ORGANISASJONSNUMMER",
                        "ident": "963743254",
                        "gjeldende": null
                      }
                    ]
                  },
                  "startdato": "2005-01-16",
                  "sluttdato": null,
                  "yrke": {
                    "kode": "7125102",
                    "beskrivelse": "BYGNINGSSNEKKER"
                  },
                  "avtaltStillingsprosent": 100,
                  "permisjonsprosent": null,
                  "permitteringsprosent": null
                }
              ]
            }
        """.trimIndent()

        install(ContentNegotiation) {
            jackson()
        }
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                this@aaregFake.log.info(
                    "SYKEPENGER :: Ukjent feil ved kall til '{}'",
                    call.request.local.uri,
                    cause
                )
                call.respond(
                    status = HttpStatusCode.InternalServerError,
                    message = ErrorRespons(cause.message)
                )
            }
        }
        routing {
            post("/api/v2/arbeidstaker/arbeidsforholdoversikt") {
                call.respond(aaregResponse)
            }
        }
    }

    private fun Application.kabalFake() {
        install(ContentNegotiation) {
            jackson()
        }
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                this@kabalFake.log.info(
                    "KABAL :: Ukjent feil ved kall til '{}'",
                    call.request.local.uri,
                    cause
                )
                call.respond(
                    status = HttpStatusCode.InternalServerError,
                    message = ErrorRespons(cause.message)
                )
            }
        }
        routing {
            post("/api/oversendelse/v4/sak") {
                call.respond(HttpStatusCode.OK)
            }
        }
    }

    private fun Application.eregFake() {
        @Language("JSON")
        val eregResponse = """
            {
              "organisasjonsnummer": "990983666",
              "type": "Virksomhet",
              "navn": {
                "sammensattnavn": "NAV IKT",
                "navnelinje1": "NAV IKT",
                "bruksperiode": {
                  "fom": "2015-02-23T08:04:53.2"
                },
                "gyldighetsperiode": {
                  "fom": "2010-04-09"
                }
              },
              "organisasjonDetaljer": {
                "registreringsdato": "2007-03-05T00:00:00",
                "enhetstyper": [
                  {
                    "enhetstype": "BEDR",
                    "bruksperiode": {
                      "fom": "2014-05-21T15:46:47.225"
                    },
                    "gyldighetsperiode": {
                      "fom": "2007-03-05"
                    }
                  }
                ],
                "navn": [
                  {
                    "sammensattnavn": "NAV IKT",
                    "navnelinje1": "NAV IKT",
                    "bruksperiode": {
                      "fom": "2015-02-23T08:04:53.2"
                    },
                    "gyldighetsperiode": {
                      "fom": "2010-04-09"
                    }
                  }
                ],
                "naeringer": [
                  {
                    "naeringskode": "84.300",
                    "hjelpeenhet": false,
                    "bruksperiode": {
                      "fom": "2014-05-22T01:18:10.661"
                    },
                    "gyldighetsperiode": {
                      "fom": "2006-07-01"
                    }
                  }
                ],
                "forretningsadresser": [
                  {
                    "type": "Forretningsadresse",
                    "adresselinje1": "Sannergata 2",
                    "postnummer": "0557",
                    "landkode": "NO",
                    "kommunenummer": "0301",
                    "bruksperiode": {
                      "fom": "2015-02-23T10:38:34.403"
                    },
                    "gyldighetsperiode": {
                      "fom": "2007-08-23"
                    }
                  }
                ],
                "postadresser": [
                  {
                    "type": "Postadresse",
                    "adresselinje1": "Postboks 5 St Olavs plass",
                    "postnummer": "0130",
                    "landkode": "NO",
                    "kommunenummer": "0301",
                    "bruksperiode": {
                      "fom": "2015-02-23T10:38:34.403"
                    },
                    "gyldighetsperiode": {
                      "fom": "2010-10-08"
                    }
                  }
                ],
                "navSpesifikkInformasjon": {
                  "erIA": true,
                  "bruksperiode": {
                    "fom": "2015-01-27T16:01:18.562"
                  },
                  "gyldighetsperiode": {
                    "fom": "2015-01-27"
                  }
                },
                "sistEndret": "2014-02-17"
              },
              "virksomhetDetaljer": {
                "enhetstype": "BEDR",
                "oppstartsdato": "2006-07-01"
              }
            }
        """.trimIndent()
        install(ContentNegotiation) {
            jackson()
        }
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                this@eregFake.log.info(
                    "EREG :: Ukjent feil ved kall til '{}'",
                    call.request.local.uri,
                    cause
                )
                call.respond(
                    status = HttpStatusCode.InternalServerError,
                    message = ErrorRespons(cause.message)
                )
            }
        }
        routing {
            get("/api/v2/organisasjon/{orgnummer}") {
                call.respond(eregResponse)
            }
        }
    }

    private fun Application.ainntektFake() {
        @Language("JSON")
        val ainntektResponse = """
            {
                "arbeidsInntektMaaned": [
                    {
                        "aarMaaned": "2025-01",
                        "arbeidsInntektInformasjon": {
                            "inntektListe": [
                                {
                                    "beloep": 4006.0,
                                    "opptjeningsland": null,
                                    "skattemessigBosattLand": null,
                                    "opptjeningsperiodeFom": "2025-01-22",
                                    "opptjeningsperiodeTom": null,
                                    "beskrivelse": "sykepenger",
                                    "virksomhet": {
                                      "identifikator": "896929119",
                                      "aktoerType": "AKTOER_ID"
                                    }
                                },
                                {
                                    "beloep": 4444.0,
                                    "opptjeningsland": null,
                                    "skattemessigBosattLand": null,
                                    "opptjeningsperiodeFom": "2025-01-01",
                                    "opptjeningsperiodeTom": null,
                                    "beskrivelse": "ikkeSykepenger",
                                    "virksomhet": {
                                      "identifikator": "896929119",
                                      "aktoerType": "AKTOER_ID"
                                    }
                                }
                            ]
                        }
                    }
                ]
            }
        """.trimIndent()
        install(ContentNegotiation) {
            jackson()
        }
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                this@ainntektFake.log.info(
                    "SYKEPENGER :: Ukjent feil ved kall til '{}'",
                    call.request.local.uri,
                    cause
                )
                call.respond(
                    status = HttpStatusCode.InternalServerError,
                    message = ErrorRespons(cause.message)
                )
            }
        }
        routing {
            post("/hentinntektliste") {
                call.respond(ainntektResponse)
            }
        }
    }

    private fun Application.datadelingFake() {
        install(ContentNegotiation) {
            jackson {
                registerModule(JavaTimeModule())
                disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            }
        }

        routing {
            post("/api/insert/meldeperioder") {
                call.respond("datadeling response")
            }
            post("/api/insert/sakStatus") {
                call.respond("datadeling response")
            }
            post("api/insert/vedtak") {
                call.respond("datadeling response")
            }
        }
    }

    private fun Application.safFake() {

        install(ContentNegotiation) {
            jackson {
                registerModule(JavaTimeModule())
                disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            }
        }

        routing {
            get("/rest/hentdokument/{journalpostId}/{dokumentInfoId}/{variantFormat}") {
                call.response.header(
                    HttpHeaders.ContentDisposition,
                    ContentDisposition.Attachment.withParameter(
                        ContentDisposition.Parameters.FileName,
                        "ktor_logo.pdf"
                    )
                        .toString()
                )
                call.response.header(HttpHeaders.ContentType, ContentType.Application.Pdf.toString())
                // Smallest possible PDF
                // https://stackoverflow.com/a/17280876/1013553
                val base64Pdf =
                    "JVBERi0xLjAKMSAwIG9iajw8L1BhZ2VzIDIgMCBSPj5lbmRvYmogMiAwIG9iajw8L0tpZHNbMyAwIFJdL0NvdW50IDE+PmVuZG9iaiAzIDAgb2JqPDwvTWVkaWFCb3hbMCAwIDMgM10+PmVuZG9iagp0cmFpbGVyPDwvUm9vdCAxIDAgUj4+Cg=="
                call.respondOutputStream {
                    val decode = Base64.getDecoder().decode(base64Pdf)
                    ByteArrayInputStream(decode).copyTo(this)
                }
            }
            post("/graphql") {
                val body = call.receive<String>()

                if ("dokumentoversiktFagsak" in body) {
                    @Language("JSON")
                    val expression = """
                            {
                              "data": {
                                "dokumentoversiktFagsak": {
                                  "journalposter": [
                                    {
                                      "journalpostId": "453877977",
                                      "journalstatus": "FERDIGSTILT",
                                      "journalposttype": "I",
                                      "behandlingstema": null,
                                      "antallRetur": null,
                                      "kanal": "NAV_NO",
                                      "innsynsregelBeskrivelse": "Standardreglene avgjør om dokumentet vises",
                                      "datoOpprettet": "2024-10-07T12:39:27",
                                      "relevanteDatoer": [],
                                      "dokumenter": [
                                        {
                                          "dokumentInfoId": "454273798",
                                          "tittel": "Søknad om Arbeidsavklaringspenger",
                                          "brevkode": "NAV 11-13.05",
                                          "dokumentstatus": null,
                                          "datoFerdigstilt": null,
                                          "originalJournalpostId": "453877971",
                                          "skjerming": null,
                                          "logiskeVedlegg": [],
                                          "dokumentvarianter": [
                                            {
                                              "variantformat": "ARKIV",
                                              "saksbehandlerHarTilgang": true,
                                              "skjerming": null
                                            },
                                            {
                                              "variantformat": "ORIGINAL",
                                              "saksbehandlerHarTilgang": true,
                                              "skjerming": null
                                            }
                                          ]
                                        },
                                        {
                                          "dokumentInfoId": "454273829",
                                          "tittel": "Annen dokumentasjon",
                                          "brevkode": null,
                                          "dokumentstatus": null,
                                          "datoFerdigstilt": null,
                                          "originalJournalpostId": "453877977",
                                          "skjerming": null,
                                          "logiskeVedlegg": [],
                                          "dokumentvarianter": [
                                            {
                                              "variantformat": "ARKIV",
                                              "saksbehandlerHarTilgang": true,
                                              "skjerming": null
                                            }
                                          ]
                                        }
                                      ]
                                    },
                                    {
                                      "journalpostId": "453873496",
                                      "journalstatus": "FERDIGSTILT",
                                      "journalposttype": "I",
                                      "behandlingstema": null,
                                      "antallRetur": null,
                                      "kanal": "NAV_NO",
                                      "innsynsregelBeskrivelse": "Standardreglene avgjør om dokumentet vises",
                                      "datoOpprettet": "2024-10-07T12:39:27",
                                      "relevanteDatoer": [],
                                      "dokumenter": [
                                        {
                                          "dokumentInfoId": "454268545",
                                          "tittel": "Søknad om Arbeidsavklaringspenger",
                                          "brevkode": "NAV 11-13.05",
                                          "dokumentstatus": null,
                                          "datoFerdigstilt": null,
                                          "originalJournalpostId": "453873496",
                                          "skjerming": null,
                                          "logiskeVedlegg": [],
                                          "dokumentvarianter": [
                                            {
                                              "variantformat": "ARKIV",
                                              "saksbehandlerHarTilgang": true,
                                              "skjerming": null
                                            },
                                            {
                                              "variantformat": "ORIGINAL",
                                              "saksbehandlerHarTilgang": true,
                                              "skjerming": null
                                            }
                                          ]
                                        }
                                      ]
                                    }
                                  ],
                                  "sideInfo": {
                                    "sluttpeker": "NDUzODczNDk2",
                                    "finnesNesteSide": false,
                                    "antall": 2,
                                    "totaltAntall": 2
                                  }
                                }
                              }
                            }
                """
                    call.respondText(
                        expression.trimIndent(),
                        contentType = ContentType.Application.Json
                    )
                } else {
                    print("FEIL KALL")
                    call.respond(HttpStatusCode.BadRequest)
                }
            }
        }
    }

    private fun Application.inst2Fake() {
        install(ContentNegotiation) {
            jackson {
                registerModule(JavaTimeModule())
            }
        }
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                this@inst2Fake.log.info("INST2 :: Ukjent feil ved kall til '{}'", call.request.local.uri, cause)
                call.respond(
                    status = HttpStatusCode.InternalServerError,
                    message = ErrorRespons(cause.message)
                )
            }
        }
        routing {
            get {
                val ident = requireNotNull(call.request.header("Nav-Personident"))
                val person = hentEllerGenererTestPerson(ident)

                val opphold = person.institusjonsopphold

                call.respond(opphold)
            }
        }
    }

    private fun Application.medlFake() {
        install(ContentNegotiation) {
            jackson()
        }
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                this@medlFake.log.info("MEDL :: Ukjent feil ved kall til '{}'", call.request.local.uri, cause)
                call.respond(
                    status = HttpStatusCode.InternalServerError,
                    message = ErrorRespons(cause.message)
                )
            }
        }

        routing {
            get {
                call.response.header(HttpHeaders.ContentType, ContentType.Application.Json.toString())

                val ident = call.request.header("Nav-Personident")

                if (ident != null) {

                    val fakePerson = FakePersoner.hentPerson(ident)

                    if (fakePerson != null) {
                        call.respond(fakePerson.medlStatus)
                    } else {
                        call.respond<List<MedlemskapResponse>>(emptyList())
                    }

                    @Suppress("UNUSED_VARIABLE")
                    @Language("JSON") val eksempelRespons =
                        """[
  {
    "unntakId": 100087727,
    "ident": "$ident",
    "fraOgMed": "2021-07-08",
    "tilOgMed": "2022-07-07",
    "status": "GYLD",
    "statusaarsak": null,
    "medlem": true,
    "grunnlag": "grunnlag",
    "lovvalg": "lovvalg",
    "lovvalgsland": "NOR"
  },
  {
    "unntakId": 100087729,
    "ident": "$ident",
    "fraOgMed": "2014-07-10",
    "tilOgMed": "2016-07-14",
    "status": "GYLD",
    "statusaarsak": null,
    "medlem": false,
    "grunnlag": "grunnlag",
    "lovvalg": "lovvalg",
    "lovvalgsland": "NOR",
    "sporingsinformasjon": {
      "versjon": 1073741824,
      "registrert": "2025-04-25",
      "besluttet": "2025-04-25",
      "kilde": "TP",
      "kildedokument": "string",
      "opprettet": "2025-04-25T09:21:22.041Z",
      "opprettetAv": "string",
      "sistEndret": "2025-04-25T09:21:22.041Z",
      "sistEndretAv": "string"
    }
  }
]"""
                } else {
                    call.respond(HttpStatusCode.BadRequest, "Mangler fødselnr i header")
                }
            }
        }
    }

    private fun Application.yrkesskadeFake() {
        install(ContentNegotiation) {
            jackson {
                registerModule(JavaTimeModule())
            }
        }

        install(StatusPages) {
            exception<Throwable> { call, cause ->
                this@yrkesskadeFake.log.info(
                    "YRKESSKADE :: Ukjent feil ved kall til '{}'",
                    call.request.local.uri,
                    cause
                )
                call.respond(
                    status = HttpStatusCode.InternalServerError,
                    message = ErrorRespons(cause.message)
                )
            }
        }
        routing {
            post("/api/v1/saker/") {
                val req = call.receive<YrkesskadeRequest>()
                val person = req.foedselsnumre.map { hentEllerGenererTestPerson(it) }

                call.respond(
                    Yrkesskader(
                        skader = person.flatMap { it.yrkesskade }
                            .map {
                                YrkesskadeModell(
                                    kommunenr = "1234",
                                    saksblokk = "A",
                                    saksnr = 1234,
                                    sakstype = "Yrkesskade",
                                    mottattdato = LocalDate.now(),
                                    resultat = "Godkjent",
                                    resultattekst = "Godkjent",
                                    vedtaksdato = LocalDate.now(),
                                    skadeart = "Arbeidsulykke",
                                    diagnose = "Kuttskade",
                                    skadedato = it.skadedato,
                                    kildetabell = "Yrkesskade",
                                    kildesystem = "Yrkesskade",
                                    saksreferanse = it.saksreferanse
                                )
                            }
                    )
                )
            }
        }
    }

    private fun barn(req: PdlRequest): PdlRelasjonDataResponse {
        val forespurtIdenter = req.variables.identer ?: emptyList()

        val barnIdenter = forespurtIdenter.mapNotNull { mapIdentBolk(it) }.toList()

        return PdlRelasjonDataResponse(
            errors = null,
            extensions = null,
            data = PdlRelasjonData(
                hentPersonBolk = barnIdenter
            )
        )
    }

    private fun mapIdentBolk(it: String): HentPersonBolkResult? {
        val person = FakePersoner.hentPerson(it)
        if (person == null) {
            return null
        }
        return HentPersonBolkResult(
            ident = person.identer.first().identifikator,
            person = PdlPersoninfo(
                foedselsdato = listOf(
                    PdlFoedsel(
                        person.fødselsdato.toFormattedString(),
                        "" + person.fødselsdato.toLocalDate().year
                    )
                ),
                doedsfall = mapDødsfall(person),
                statsborgerskap = setOf(PdlStatsborgerskap("NOR", LocalDate.now().minusYears(5), LocalDate.now())),
                folkeregisterpersonstatus = setOf(PdlFolkeregisterPersonStatus(PersonStatus.bosatt, null))
            )
        )
    }

    private fun mapDødsfall(person: TestPerson): Set<PDLDødsfall>? {
        if (person.dødsdato == null) {
            return null
        }
        return setOf(PDLDødsfall(person.dødsdato.toFormatedString()))
    }

    private fun barnRelasjoner(req: PdlRequest): PdlRelasjonDataResponse {
        val testPerson = hentEllerGenererTestPerson(req.variables.ident ?: "")
        return PdlRelasjonDataResponse(
            errors = null,
            extensions = null,
            data = PdlRelasjonData(
                hentPerson = PdlPersoninfo(
                    forelderBarnRelasjon = testPerson.barn
                        .map { PdlRelasjon(it.identer.first().identifikator) }
                        .toList(),
                    statsborgerskap = setOf(PdlStatsborgerskap("NOR", LocalDate.now().minusYears(5), LocalDate.now())),
                    folkeregisterpersonstatus = setOf(PdlFolkeregisterPersonStatus(PersonStatus.bosatt, null))
                )
            )
        )
    }

    private fun identer(req: PdlRequest): PdlIdenterDataResponse {
        val person = hentEllerGenererTestPerson(req.variables.ident ?: "")

        return PdlIdenterDataResponse(
            errors = null,
            extensions = null,
            data = PdlIdenterData(
                hentIdenter = PdlIdenter(
                    identer = mapIdent(person)
                )
            ),
        )
    }

    // TODO!?
    private fun hentEllerGenererTestPerson(forespurtIdent: String): TestPerson {
        val person = FakePersoner.hentPerson(forespurtIdent)
        if (person == null) {
            FakePersoner.leggTil(
                TestPerson(
                    identer = setOf(Ident(forespurtIdent)),
                    fødselsdato = Fødselsdato(LocalDate.now().minusYears(30))
                )
            )
        }

        return FakePersoner.hentPerson(forespurtIdent)!!
    }

    private fun mapIdent(person: TestPerson?): List<PdlIdent> {
        if (person == null) {
            return emptyList()
        }
        return listOf(
            PdlIdent(
                person.identer.first().identifikator,
                false,
                PdlGruppe.FOLKEREGISTERIDENT
            )
        )
    }

    private fun personopplysninger(req: PdlRequest): PdlPersoninfoDataResponse {
        val testPerson = hentEllerGenererTestPerson(req.variables.ident ?: "")
        return PdlPersoninfoDataResponse(
            errors = null,
            extensions = null,
            data = PdlPersoninfoData(
                hentPerson = PdlPersoninfo(
                    foedselsdato = listOf(
                        PdlFoedsel(
                            testPerson.fødselsdato.toFormattedString(),
                            "" + testPerson.fødselsdato.toLocalDate().year
                        )
                    ),
                    statsborgerskap = setOf(PdlStatsborgerskap("NOR", LocalDate.now(), LocalDate.now())),
                    folkeregisterpersonstatus = setOf(PdlFolkeregisterPersonStatus(PersonStatus.bosatt, null))
                )
            ),
        )
    }

    private fun personopplysningerHistorikk(req: PdlRequest): PdlPersoninfoDataResponse {
        val testPerson = hentEllerGenererTestPerson(req.variables.ident ?: "")
        return PdlPersoninfoDataResponse(
            errors = null,
            extensions = null,
            data = PdlPersoninfoData(
                hentPerson = testPerson.let { person ->
                    PdlPersoninfo(
                        foedselsdato = listOf(
                            PdlFoedsel(
                                person.fødselsdato.toFormattedString(),
                                "" + person.fødselsdato.toLocalDate().year
                            )
                        ),
                        statsborgerskap = person.statsborgerskap.toSet(),
                        folkeregisterpersonstatus = person.personStatus.toSet(),
                    )
                }
            ),
        )
    }

    private fun navn(req: PdlRequest): PdlPersonNavnDataResponse {
        val testPerson = hentEllerGenererTestPerson(req.variables.ident ?: "")
        return PdlPersonNavnDataResponse(
            errors = null,
            extensions = null,
            data = HentPerson(
                hentPerson = PdlNavnData(
                    ident = testPerson.identer.first().identifikator,
                    navn = listOf(
                        PdlNavn(
                            fornavn = testPerson.navn.fornavn,
                            mellomnavn = null,
                            etternavn = testPerson.navn.etternavn
                        )
                    )
                )
            ),
        )
    }

    private fun bolknavn(req: PdlRequest): PdlPersonNavnDataResponse {
        val navnData = req.variables.identer?.map {
            val testPerson = hentEllerGenererTestPerson(it)
            PdlNavnDataBolk(
                ident = testPerson.identer.first().identifikator,
                person =
                    PdlPersonBolk(
                        navn = listOf(
                            PdlNavn(
                                fornavn = testPerson.navn.fornavn,
                                mellomnavn = null,
                                etternavn = testPerson.navn.etternavn
                            )
                        )
                    )
            )
        }

        return PdlPersonNavnDataResponse(
            errors = null,
            extensions = null,
            data = HentPerson(hentPersonBolk = navnData)
        )
    }

    private fun Application.azureFake() {
        install(ContentNegotiation) {
            jackson()
        }
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                this@azureFake.log.info("AZURE :: Ukjent feil ved kall til '{}'", call.request.local.uri, cause)
                call.respond(
                    status = HttpStatusCode.InternalServerError,
                    message = ErrorRespons(cause.message)
                )
            }
        }
        routing {
            post("/token/{NAVident}") {
                val body = call.receiveText()
                val NAVident = call.parameters["NAVident"]
                val token = AzureTokenGen(
                    issuer = "behandlingsflyt",
                    audience = "behandlingsflyt"
                ).generate(body.contains("grant_type=client_credentials"), azp = "behandlingsflyt", NAVident)
                call.respond(TestToken(access_token = token))
            }
            get("/jwks") {
                call.respond(AZURE_JWKS)
            }
        }
    }

    private fun Application.utbetalFake() {
        install(ContentNegotiation) {
            jackson {
                registerModule(JavaTimeModule())
            }
        }
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                this@utbetalFake.log.info("Utbetal :: Ukjent feil ved kall til '{}'", call.request.local.uri, cause)
                call.respond(
                    status = HttpStatusCode.InternalServerError,
                    message = ErrorRespons(cause.message)
                )
            }

        }

        routing {
            post("/tilkjentytelse") {
                call.respond(HttpStatusCode.NoContent)
            }
        }

    }

    private fun Application.meldekortFake() {
        install(ContentNegotiation) {
            jackson {
                registerModule(JavaTimeModule())
            }
        }
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                this@meldekortFake.log.info("Meldekort :: Ukjent feil ved kall til '{}'", call.request.local.uri, cause)
                call.respond(
                    status = HttpStatusCode.InternalServerError,
                    message = ErrorRespons(cause.message)
                )
            }

        }

        routing {
            post("/api/behandlingsflyt/sak/meldeperioder") {
                call.receive<MeldeperioderV0>()
                call.respond(HttpStatusCode.NoContent)
            }
        }

    }

    private fun Application.nomFake() {
        install(ContentNegotiation) {
            jackson {
                registerModule(JavaTimeModule())
            }
        }
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                this@nomFake.log.info("Nom :: Ukjent feil ved kall til '{}'", call.request.local.uri, cause)
                call.respond(
                    status = HttpStatusCode.InternalServerError,
                    message = ErrorRespons(cause.message)
                )
            }

        }

        routing {
            post("/graphql") {
                val data = NomData(
                    NomDataRessurs(
                        orgTilknytning = listOf(
                            OrgTilknytning(
                                OrgEnhet("1234"),
                                true,
                                LocalDate.now(),
                                null
                            )
                        ), visningsnavn = "Test Testesen"
                    )
                )
                val response = GraphQLResponse(
                    data,
                    emptyList()
                )

                call.respond(response)
            }
        }
    }


    private fun Application.norgFake() {
        install(ContentNegotiation) {
            jackson {
                registerModule(JavaTimeModule())
            }
        }
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                this@norgFake.log.info("Norg :: Ukjent feil ved kall til '{}'", call.request.local.uri, cause)
                call.respond(
                    status = HttpStatusCode.InternalServerError,
                    message = ErrorRespons(cause.message)
                )
            }
        }

        routing {
            get("/norg2/api/v1/enhet/{enhetsnummer}") {
                call.respond(NorgEnhet("1234", "Lokalenhetsnavn", "LOKAL"))
            }
        }
    }

    private fun Application.brevFake() {
        val config = ClientConfig(scope = "")
        val client = RestClient.withDefaultResponseHandler(
            config = config,
            tokenProvider = ClientCredentialsTokenProvider
        )
        install(ContentNegotiation) {
            register(
                ContentType.Application.Json,
                JacksonConverter(objectMapper = DefaultJsonMapper.objectMapper(), true)
            )
        }
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                this@brevFake.log.info("BREV :: Ukjent feil ved kall til '{}'", call.request.local.uri, cause)
                call.respond(
                    status = HttpStatusCode.InternalServerError,
                    message = ErrorRespons(cause.message)
                )
            }
        }

        val brevStore = mutableListOf<BrevbestillingResponse>()
        val mutex = Any()
        fun brev(
            brevbestillingReferanse: UUID,
            status: Status,
            brevtype: Brevtype,
        ) = BrevbestillingResponse(
            referanse = brevbestillingReferanse,
            brev = Brev(
                kanSendesAutomatisk = false,
                journalpostTittel = "En tittel",
                overskrift = "Overskrift H1",
                kanOverstyreBrevtittel = false,
                tekstbolker = listOf(
                    Tekstbolk(
                        id = UUID.randomUUID(),
                        overskrift = "Overskrift H2",
                        innhold = listOf(
                            Innhold(
                                id = UUID.randomUUID(),
                                overskrift = "Overskrift H3",
                                blokker = emptyList(),
                                kanRedigeres = true,
                                erFullstendig = false
                            )
                        )
                    )
                )
            ),
            opprettet = LocalDateTime.now(),
            oppdatert = LocalDateTime.now(),
            behandlingReferanse = UUID.randomUUID(),
            brevtype = brevtype,
            språk = Språk.NB,
            status = status,
        )


        routing {
            route("/api") {
                route("/v2") {
                    post("/bestill") {
                        val request = call.receive<BestillBrevV2Request>()
                        val brevbestillingReferanse = UUID.randomUUID()

                        val status = if (request.ferdigstillAutomatisk) {
                            Status.FERDIGSTILT
                        } else {
                            Status.UNDER_ARBEID
                        }
                        synchronized(mutex) {
                            brevStore += brev(brevbestillingReferanse, status, request.brevtype)
                        }
                        call.respond(status = HttpStatusCode.Created, BestillBrevResponse(brevbestillingReferanse))
                    }
                }
                route("/bestilling/{referanse}") { ->
                    get {
                        val ref = UUID.fromString(call.pathParameters.get("referanse"))!!

                        call.respond(
                            synchronized(mutex) {
                                brevStore.find { it.referanse == ref }!!
                            }
                        )
                    }
                    post("/forhandsvis") {
                        call.respond(
                            this.javaClass.classLoader.getResourceAsStream("sample.pdf")?.readAllBytes() ?: ByteArray(0)
                        )
                    }
                    put("/oppdater") {
                        val ref = UUID.fromString(call.pathParameters.get("referanse"))!!
                        val brev = call.receive<Brev>()
                        synchronized(mutex) {
                            val i = brevStore.indexOfFirst { it.referanse == ref }
                            brevStore[i] = brevStore[i].copy(brev = brev)
                        }
                        call.respond(HttpStatusCode.NoContent, Unit)
                    }
                }
                post("/ferdigstill") {
                    val ref = call.receive<FerdigstillBrevRequest>().referanse
                    synchronized(mutex) {
                        val i = brevStore.indexOfFirst { it.referanse == ref }
                        brevStore[i] = brevStore[i].copy(status = Status.FERDIGSTILT)
                    }
                    call.respond(HttpStatusCode.Accepted, Unit)
                }
                post("/forhandsvis-signaturer") {
                    val request = call.receive<HentSignaturerRequest>()

                    val signaturer = request.signaturGrunnlag.map {
                        Signatur(
                            navn = "Navn ${it.navIdent}",
                            enhet = "Nav Enheten"
                        )
                    }
                    val response = HentSignaturerResponse(signaturer)
                    call.respond(response)
                }
            }
        }
    }

    @Suppress("PropertyName")
    data class TestToken(
        val access_token: String,
        val refresh_token: String = "very.secure.token",
        val id_token: String = "very.secure.token",
        val token_type: String = "token-type",
        val scope: String? = null,
        val expires_in: Int = 3599,
    )


    fun start() {
        if (started.get()) {
            return
        }

        azure.start()
        setAzureProperties()
        brev.start()
        yrkesskade.start()
        pdl.start()
        inntekt.start()
        oppgavestyring.start()
        saf.start()
        inst2.start()
        sam.start()
        medl.start()
        tilgang.start()
        foreldrepenger.start()
        pesysFake.start()
        sykepenger.start()
        statistikk.start()
        dokumentinnhenting.start()
        ainntekt.start()
        aareg.start()
        datadeling.start()
        utbetal.start()
        meldekort.start()
        tjenestePensjon.start()
        unleash.start()
        nom.start()
        norg.start()
        kabal.start()
        ereg.start()

        println("AZURE PORT ${azure.port()}")

        setProperties()

        started.set(true)
    }

    private fun setAzureProperties() {
        System.setProperty("azure.openid.config.token.endpoint", "http://localhost:${azure.port()}/token/x12345")
        System.setProperty("azure.app.client.id", "behandlingsflyt")
        System.setProperty("azure.app.client.secret", "")
        System.setProperty("azure.openid.config.jwks.uri", "http://localhost:${azure.port()}/jwks")
        System.setProperty("azure.openid.config.issuer", "behandlingsflyt")
    }

    private fun setProperties() {
        System.setProperty("nais.app.name", "behandlingsflyt")

        // Brev
        System.setProperty("integrasjon.brev.url", "http://localhost:${brev.port()}")
        System.setProperty("integrasjon.brev.scope", "brev")

        // Pdl
        System.setProperty("integrasjon.pdl.url", "http://localhost:${pdl.port()}")
        System.setProperty("integrasjon.pdl.scope", "pdl")

        //popp
        System.setProperty("integrasjon.inntekt.url", "http://localhost:${inntekt.port()}")
        System.setProperty("integrasjon.inntekt.scope", "popp")


        // Yrkesskade
        System.setProperty("integrasjon.yrkesskade.url", "http://localhost:${yrkesskade.port()}")
        System.setProperty("integrasjon.yrkesskade.scope", "yrkesskade")

        // Oppgavestyring
        System.setProperty("integrasjon.oppgavestyring.scope", "oppgavestyring")
        System.setProperty("integrasjon.oppgavestyring.url", "http://localhost:${oppgavestyring.port()}")

        // Saf
        System.setProperty("integrasjon.saf.url.graphql", "http://localhost:${saf.port()}/graphql")
        System.setProperty("integrasjon.saf.scope", "saf")
        System.setProperty("integrasjon.saf.url.rest", "http://localhost:${saf.port()}/rest")

        // MEDL
        System.setProperty("integrasjon.medl.url", "http://localhost:${medl.port()}")
        System.setProperty("integrasjon.medl.scope", "medl")

        // Inst
        System.setProperty("integrasjon.institusjonsopphold.url", "http://localhost:${inst2.port()}")
        System.setProperty("integrasjon.institusjonsopphold.scope", "inst2")

        // Statistikk-app
        System.setProperty("integrasjon.statistikk.url", "http://localhost:${statistikk.port()}")
        System.setProperty("integrasjon.statistikk.scope", "statistikk")

        // Pesys
        System.setProperty("integrasjon.pesys.url", "http://localhost:${pesysFake.port()}")
        System.setProperty("integrasjon.pesys.scope", "scope")

        // Tilgang
        System.setProperty("integrasjon.tilgang.url", "http://localhost:${tilgang.port()}")
        System.setProperty("integrasjon.tilgang.scope", "scope")

        // Foreldrepenger
        System.setProperty("integrasjon.foreldrepenger.url", "http://localhost:${foreldrepenger.port()}")
        System.setProperty("integrasjon.foreldrepenger.scope", "scope")

        // Sykepenger
        System.setProperty("integrasjon.sykepenger.url", "http://localhost:${sykepenger.port()}")
        System.setProperty("integrasjon.sykepenger.scope", "scope")

        // Dokumentinnhenting
        System.setProperty("integrasjon.dokumentinnhenting.url", "http://localhost:${dokumentinnhenting.port()}")
        System.setProperty("integrasjon.dokumentinnhenting.scope", "scope")

        // AAregisteret
        System.setProperty("integrasjon.aareg.url", "http://localhost:${aareg.port()}")
        System.setProperty("integrasjon.aareg.scope", "scope")

        // Inntektskomponenten
        System.setProperty("integrasjon.inntektskomponenten.url", "http://localhost:${ainntekt.port()}")
        System.setProperty("integrasjon.inntektskomponenten.scope", "scope")

        // Datadeling
        System.setProperty("integrasjon.datadeling.url", "http://localhost:${datadeling.port()}")
        System.setProperty("integrasjon.datadeling.scope", "scope")

        // Utbetal
        System.setProperty("integrasjon.utbetal.url", "http://localhost:${utbetal.port()}")
        System.setProperty("integrasjon.utbetal.scope", "utbetal")

        // Meldekort
        System.setProperty("integrasjon.meldekort.url", "http://localhost:${meldekort.port()}")
        System.setProperty("integrasjon.meldekort.scope", "meldekort")

        //tjenestepensjon
        System.setProperty("integrasjon.tjenestepensjon.url", "http://localhost:${tjenestePensjon.port()}")
        System.setProperty("integrasjon.tjenestepensjon.scope", "tjenestepensjon")

        //unleash
        System.setProperty("nais.app.name", "behandlingsflyt")
        System.setProperty("unleash.server.api.url", "http://localhost:${unleash.port()}")
        System.setProperty("unleash.server.api.token", "token-behandlingsflyt-unleash")

        //Azp
        System.setProperty("integrasjon.tilgang.azp", UUID.randomUUID().toString())
        System.setProperty("integrasjon.brev.azp", UUID.randomUUID().toString())
        System.setProperty("integrasjon.dokumentinnhenting.azp", UUID.randomUUID().toString())
        System.setProperty("integrasjon.postmottak.azp", UUID.randomUUID().toString())
        System.setProperty("integrasjon.saksbehandling.azp", UUID.randomUUID().toString())

        // Norg
        System.setProperty("integrasjon.norg.url", "http://localhost:${norg.port()}")

        // NOM
        System.setProperty("integrasjon.nom.url", "http://localhost:${nom.port()}/graphql")
        System.setProperty("integrasjon.nom.scope", "scope")

        // Kabal
        System.setProperty("integrasjon.kabal.url", "http://localhost:${kabal.port()}")
        System.setProperty("integrasjon.kabal.scope", "scope")

        //Enhetsregisteret
        System.setProperty("integrasjon.ereg.url", "http://localhost:${ereg.port()}")
        System.setProperty("integrasjon.ereg.scope", "scope")

        // Sam
        System.setProperty("integrasjon.sam.url", "http://localhost:${sam.port()}")
        System.setProperty("integrasjon.sam.scope", "sam")
    }

    override fun close() {
        log.info("Closing Servers.")
        if (!started.get()) {
            return
        }
        yrkesskade.stop(0L, 0L)
        pdl.stop(0L, 0L)
        azure.stop(0L, 0L)
        brev.stop(0L, 0L)
        inntekt.stop(0L, 0L)
        oppgavestyring.stop(0L, 0L)
        saf.stop(0L, 0L)
        inst2.stop(0L, 0L)
        medl.stop(0L, 0L)
        tilgang.stop(0L, 0L)
        foreldrepenger.stop(0L, 0L)
        sykepenger.stop(0L, 0L)
        dokumentinnhenting.stop(0L, 0L)
        ainntekt.stop(0L, 0L)
        aareg.stop(0L, 0L)
        datadeling.stop(0L, 0L)
        utbetal.stop(0L, 0L)
        meldekort.stop(0L, 0L)
        tjenestePensjon.stop(0L, 0L)
        unleash.stop(0L, 0L)
        nom.stop(0L, 0L)
        norg.stop(0L, 0L)
        kabal.stop(0L, 0L)
        ereg.stop(0L, 0L)
    }
}

private fun EmbeddedServer<*, *>.port(): Int {
    return runBlocking {
        this@port.engine.resolvedConnectors()
    }.first { it.type == ConnectorType.HTTP }
        .port
}

object AzurePortHolder {
    private val azurePort = AtomicInteger(0)

    fun setPort(port: Int) {
        azurePort.set(port)
    }

    fun getPort(): Int {
        return azurePort.get()
    }
}