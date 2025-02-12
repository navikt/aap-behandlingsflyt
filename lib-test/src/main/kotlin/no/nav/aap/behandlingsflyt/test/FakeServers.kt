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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import no.nav.aap.behandlingsflyt.behandling.dokumentinnhenting.BestillLegeerklæringDto
import no.nav.aap.behandlingsflyt.behandling.dokumentinnhenting.ForhåndsvisBrevRequest
import no.nav.aap.behandlingsflyt.behandling.dokumentinnhenting.HentStatusLegeerklæring
import no.nav.aap.behandlingsflyt.behandling.dokumentinnhenting.PurringLegeerklæring
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.SykepengerRequest
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.SykepengerResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.UtbetaltePerioder
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.dokumentinnhenting.LegeerklæringStatusResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.dokumentinnhenting.MeldingStatusType
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.adapter.InntektRequest
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.adapter.InntektResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.adapter.SumPi
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.adapter.PERSON_QUERY
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.adapter.UføreRespons
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.adapter.YrkesskadeModell
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.adapter.YrkesskadeRequest
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.adapter.Yrkesskader
import no.nav.aap.behandlingsflyt.integrasjon.barn.BARN_RELASJON_QUERY
import no.nav.aap.behandlingsflyt.integrasjon.barn.PERSON_BOLK_QUERY
import no.nav.aap.behandlingsflyt.integrasjon.ident.IDENT_QUERY
import no.nav.aap.behandlingsflyt.integrasjon.ident.PdlPersoninfoGateway
import no.nav.aap.behandlingsflyt.kontrakt.brevbestilling.BrevbestillingLøsningStatus
import no.nav.aap.behandlingsflyt.kontrakt.brevbestilling.LøsBrevbestillingDto
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
import no.nav.aap.behandlingsflyt.test.modell.TestPerson
import no.nav.aap.brev.kontrakt.BestillBrevRequest
import no.nav.aap.brev.kontrakt.BestillBrevResponse
import no.nav.aap.brev.kontrakt.Brev
import no.nav.aap.brev.kontrakt.BrevbestillingResponse
import no.nav.aap.brev.kontrakt.Brevtype
import no.nav.aap.brev.kontrakt.Innhold
import no.nav.aap.brev.kontrakt.Språk
import no.nav.aap.brev.kontrakt.Status
import no.nav.aap.brev.kontrakt.Tekstbolk
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.tilgang.BehandlingTilgangRequest
import no.nav.aap.tilgang.JournalpostTilgangRequest
import no.nav.aap.tilgang.SakTilgangRequest
import no.nav.aap.tilgang.TilgangResponse
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.net.URI
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Year
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

private val logger = LoggerFactory.getLogger(FakesExtension::class.java)

object FakeServers : AutoCloseable {
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
                    status = HttpStatusCode.Companion.InternalServerError,
                    message = ErrorRespons(cause.message)
                )
            }
        }
        routing {
            post("/oppdater-oppgaver") {
                call.respond(HttpStatusCode.Companion.NoContent)
            }
        }
    }

    private fun Application.pesysFake() {
        install(ContentNegotiation) {
            jackson()
        }
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                this@pesysFake.log.info("Inntekt :: Ukjent feil ved kall til '{}'", call.request.local.uri, cause)
                call.respond(
                    status = HttpStatusCode.Companion.InternalServerError,
                    message = ErrorRespons(cause.message)
                )
            }
        }
        routing() {
            get("/pen/api/uforetrygd/uforegrad") {
                val ident = requireNotNull(call.request.header("fnr"))
                val hentPerson = FakePersoner.hentPerson(ident)
                if (hentPerson == null) {
                    call.respond(HttpStatusCode.Companion.NotFound, "Fant ikke person med fnr $ident")
                    return@get
                }
                val uføregrad = hentPerson.uføre?.prosentverdi()
                if (uføregrad == null) {
                    call.respond(HttpStatusCode.OK, UføreRespons(uforegrad = 0))
                } else {
                    call.respond(HttpStatusCode.Companion.OK, UføreRespons(uforegrad = uføregrad))
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
                    status = HttpStatusCode.Companion.InternalServerError,
                    message = ErrorRespons(cause.message)
                )
            }
        }
        routing {
            post {
                val req = call.receive<InntektRequest>()
                val person = hentEllerGenererTestPerson(req.fnr)

                for (år in req.fomAr..req.tomAr) {
                    person.leggTilInntektHvisÅrMangler(Year.of(år), Beløp("0"))
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
                    status = HttpStatusCode.Companion.InternalServerError,
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
                    PdlPersoninfoGateway.PERSONINFO_QUERY -> call.respond(navn(req))
                    PdlPersoninfoGateway.PERSONINFO_BOLK_QUERY -> call.respond(bolknavn(req))
                    BARN_RELASJON_QUERY -> call.respond(barnRelasjoner(req))
                    PERSON_BOLK_QUERY -> call.respond(barn(req))
                    else -> call.respond(HttpStatusCode.Companion.BadRequest)
                }
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
                    status = HttpStatusCode.Companion.InternalServerError,
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
                call.respond(TilgangResponse(true))
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
                    status = HttpStatusCode.Companion.InternalServerError,
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
            post("/api/dokumentinnhenting/syfo/purring/{dialogmelding}") {
                call.receive<PurringLegeerklæring>()
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
                    status = HttpStatusCode.Companion.InternalServerError,
                    message = ErrorRespons(cause.message)
                )
            }
        }
        routing {
            post("/stoppetBehandling") {
                val receive = call.receive<StoppetBehandling>()
                statistikkHendelser.add(receive)
                call.respond(status = HttpStatusCode.Companion.Accepted, message = "{}")
            }
        }
    }

    private fun Application.fpFake() {
        @Language("JSON")
        val foreldrepengerOgSvangerskapspengerResponse = """
           [{
            "aktor": {
                "verdi": 2325108100427
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

        install(ContentNegotiation) {
            jackson()
        }
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                this@fpFake.log.info(
                    "FORELDREPENGER :: Ukjent feil ved kall til '{}'",
                    call.request.local.uri,
                    cause
                )
                call.respond(
                    status = HttpStatusCode.Companion.InternalServerError,
                    message = ErrorRespons(cause.message)
                )
            }
        }
        routing {
            post("/hent-ytelse-vedtak") {
                call.receive<String>()
                call.respond(foreldrepengerOgSvangerskapspengerResponse)
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
                            utbetaltePerioder = fakePerson.sykepenger.map {
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
                    status = HttpStatusCode.Companion.InternalServerError,
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
                    status = HttpStatusCode.Companion.InternalServerError,
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
                    ContentDisposition.Companion.Attachment.withParameter(
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
                    call.respond(HttpStatusCode.Companion.BadRequest)
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
                    status = HttpStatusCode.Companion.InternalServerError,
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
                    status = HttpStatusCode.Companion.InternalServerError,
                    message = ErrorRespons(cause.message)
                )
            }
        }

        routing {
            get {
                call.response.header(HttpHeaders.ContentType, ContentType.Application.Json.toString())

                @Language("JSON") val respons =
                    """[
  {
    "unntakId": 100087727,
    "ident": "02429118789",
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
    "ident": "02429118789",
    "fraOgMed": "2014-07-10",
    "tilOgMed": "2016-07-14",
    "status": "GYLD",
    "statusaarsak": null,
    "medlem": false,
    "grunnlag": "grunnlag",
    "lovvalg": "lovvalg",
    "lovvalgsland": "NOR"
  }
]"""

                call.respond(
                    respons
                )
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
                    status = HttpStatusCode.Companion.InternalServerError,
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
                        person.fødselsdato.toFormatedString(),
                        "" + person.fødselsdato.toLocalDate().year
                    )
                ),
                doedsfall = mapDødsfall(person),
                statsborgerskap = setOf(PdlStatsborgerskap("NOR", LocalDate.now().minusYears(5), LocalDate.now())),
                folkeregisterpersonstatus = setOf(PdlFolkeregisterPersonStatus(PersonStatus.bosatt))
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
                    folkeregisterpersonstatus = setOf(PdlFolkeregisterPersonStatus(PersonStatus.bosatt))
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
                hentPerson = mapPerson(testPerson)
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
            PdlNavnData(
                ident = testPerson.identer.first().identifikator,
                navn = listOf(
                    PdlNavn(
                        fornavn = testPerson.navn.fornavn,
                        mellomnavn = null,
                        etternavn = testPerson.navn.etternavn
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

    private fun mapPerson(person: TestPerson?): PdlPersoninfo? {
        if (person == null) {
            return null
        }
        return PdlPersoninfo(
            foedselsdato = listOf(
                PdlFoedsel(
                    person.fødselsdato.toFormatedString(),
                    "" + person.fødselsdato.toLocalDate().year
                )
            ),
            statsborgerskap = setOf(PdlStatsborgerskap("NOR", LocalDate.now(), LocalDate.now())),
            folkeregisterpersonstatus = setOf(PdlFolkeregisterPersonStatus(PersonStatus.bosatt))
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
                    status = HttpStatusCode.Companion.InternalServerError,
                    message = ErrorRespons(cause.message)
                )
            }
        }
        routing {
            post("/token") {
                val body = call.receiveText()
                val token = AzureTokenGen(
                    issuer = "behandlingsflyt",
                    audience = "behandlingsflyt"
                ).generate(body.contains("grant_type=client_credentials"))
                call.respond(TestToken(access_token = token))
            }
            get("/jwks") {
                call.respond(AZURE_JWKS)
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
                    status = HttpStatusCode.Companion.InternalServerError,
                    message = ErrorRespons(cause.message)
                )
            }
        }

        val brevStore = mutableListOf<BrevbestillingResponse>()
        val mutex = Any()

        routing {
            route("/api") {
                post("/bestill") {
                    val request = call.receive<BestillBrevRequest>()
                    val brevbestillingReferanse = UUID.randomUUID()

                    synchronized(mutex) {
                        brevStore += BrevbestillingResponse(
                            referanse = brevbestillingReferanse,
                            brev = Brev(
                                journalpostTittel = "En tittel",
                                overskrift = "Overskrift", tekstbolker = listOf(
                                    Tekstbolk(
                                        id = UUID.randomUUID(),
                                        overskrift = "En fin overskrift",
                                        innhold = listOf(
                                            Innhold(
                                                id = UUID.randomUUID(),
                                                overskrift = "Enda en overskrift",
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
                            brevtype = Brevtype.INNVILGELSE,
                            språk = Språk.NB,
                            status = Status.REGISTRERT,
                        )
                    }

                    call.respond(status = HttpStatusCode.Created, BestillBrevResponse(brevbestillingReferanse))

                    launch {
                        delay(100)
                        synchronized(mutex) {
                            val i = brevStore.indexOfFirst { it.referanse == brevbestillingReferanse }
                            brevStore[i] = brevStore[i].copy(status = Status.UNDER_ARBEID)
                        }

                        val responseRequest = LøsBrevbestillingDto(
                            behandlingReferanse = request.behandlingReferanse,
                            bestillingReferanse = brevbestillingReferanse,
                            status = BrevbestillingLøsningStatus.KLAR_FOR_EDITERING,
                        )
                        val uri = URI.create("http://localhost:8080/api/brev/los-bestilling")
                        val httpRequest = PostRequest(
                            body = responseRequest,
                            additionalHeaders = listOf(
                                Header("Accept", "application/json")
                            )
                        )
                        client.post<_, Unit>(uri = uri, request = httpRequest)
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
                    put("/oppdater") {
                        call.respond(HttpStatusCode.NoContent, Unit)
                    }
                }
                post("/ferdigstill") {
                    call.respond(HttpStatusCode.Accepted, Unit)
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
        medl.start()
        tilgang.start()
        foreldrepenger.start()
        pesysFake.start()
        sykepenger.start()
        statistikk.start()
        dokumentinnhenting.start()
        ainntekt.start()
        aareg.start()

        println("AZURE PORT ${azure.port()}")

        setProperties()

        started.set(true)
    }

    private fun setAzureProperties() {
        System.setProperty("azure.openid.config.token.endpoint", "http://localhost:${azure.port()}/token")
        System.setProperty("azure.app.client.id", "behandlingsflyt")
        System.setProperty("azure.app.client.secret", "")
        System.setProperty("azure.openid.config.jwks.uri", "http://localhost:${azure.port()}/jwks")
        System.setProperty("azure.openid.config.issuer", "behandlingsflyt")
    }

    private fun setProperties() {

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
    }

    override fun close() {
        logger.info("Closing Servers.")
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