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
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import no.nav.aap.Inntekt.InntektRequest
import no.nav.aap.Inntekt.InntektResponse
import no.nav.aap.Inntekt.SumPi
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.adapter.BARN_RELASJON_QUERY
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.adapter.PERSON_BOLK_QUERY
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.adapter.PERSON_QUERY
import no.nav.aap.behandlingsflyt.kontrakt.brevbestilling.BrevbestillingLøsningStatus
import no.nav.aap.behandlingsflyt.kontrakt.brevbestilling.LøsBrevbestillingDto
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.StoppetBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.IDENT_QUERY
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.PdlPersoninfoGateway
import no.nav.aap.behandlingsflyt.test.modell.TestPerson
import no.nav.aap.brev.kontrakt.BestillBrevRequest
import no.nav.aap.brev.kontrakt.BestillBrevResponse
import no.nav.aap.brev.kontrakt.Brev
import no.nav.aap.brev.kontrakt.BrevbestillingResponse
import no.nav.aap.brev.kontrakt.Brevtype
import no.nav.aap.brev.kontrakt.Språk
import no.nav.aap.brev.kontrakt.Status
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.pdl.HentPerson
import no.nav.aap.pdl.HentPersonBolkResult
import no.nav.aap.pdl.PDLDødsfall
import no.nav.aap.pdl.PdlFoedsel
import no.nav.aap.pdl.PdlGruppe
import no.nav.aap.pdl.PdlIdent
import no.nav.aap.pdl.PdlIdenter
import no.nav.aap.pdl.PdlIdenterData
import no.nav.aap.pdl.PdlIdenterDataResponse
import no.nav.aap.pdl.PdlNavn
import no.nav.aap.pdl.PdlNavnData
import no.nav.aap.pdl.PdlPersonNavnDataResponse
import no.nav.aap.pdl.PdlPersoninfo
import no.nav.aap.pdl.PdlPersoninfoData
import no.nav.aap.pdl.PdlPersoninfoDataResponse
import no.nav.aap.pdl.PdlRelasjon
import no.nav.aap.pdl.PdlRelasjonData
import no.nav.aap.pdl.PdlRelasjonDataResponse
import no.nav.aap.pdl.PdlRequest
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.verdityper.sakogbehandling.Ident
import no.nav.aap.yrkesskade.YrkesskadeModell
import no.nav.aap.yrkesskade.YrkesskadeRequest
import no.nav.aap.yrkesskade.Yrkesskader
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import tilgang.BehandlingTilgangRequest
import tilgang.JournalpostTilgangRequest
import tilgang.SakTilgangRequest
import tilgang.TilgangResponse
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

    internal val statistikkHendelser = mutableListOf<StoppetBehandling>()

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
            get("/vedtak/gradalderellerufore") {
                val ident = requireNotNull(call.request.header("Nav-Personident"))
                val uføregrad = FakePersoner.hentPerson(ident)?.uføre?.prosentverdi() ?: 0

                call.respond(HttpStatusCode.Companion.OK, uføregrad)
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
            jackson()
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
        @Language("JSON")
        val spResponse = """
        {
            "utbetaltePerioder": [
                { "personidentifikator": "11111111111", "grad": 100, "fom": "2018-01-01", "tom": "2018-01-10" },
                { "personidentifikator": "11111111112", "grad": 70, "fom": "2018-01-11", "tom": "2018-01-20" },
                { "personidentifikator": "11111111113", "grad": 60, "fom": "2018-01-21", "tom": "2018-01-31" },
                { "personidentifikator": "11111111114", "grad": 50, "fom": "2018-02-01", "tom": "2018-02-10" }
            ]
        }
        """.trimIndent()

        install(ContentNegotiation) {
            jackson()
        }
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                this@spFake.log.info(
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
            post("/utbetalte-perioder-aap") {
                call.receive<String>()
                call.respond(spResponse)
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
                                          "originalJournalpostId": "453877977",
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
    "lovvalg": "lovvalg"
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
    "lovvalg": "lovvalg"
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
                doedsfall = mapDødsfall(person)
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
                        .toList()
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
            )
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
                val token = AzureTokenGen("behandlingsflyt", "behandlingsflyt").generate()
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
            jackson()
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
        routing {
            route("/api") {
                post("/bestill") {
                    val request = call.receive<BestillBrevRequest>()
                    val brevbestillingReferanse = UUID.randomUUID()

                    call.respond(status = HttpStatusCode.Created, BestillBrevResponse(brevbestillingReferanse))

                    launch {
                        delay(100)
                        val responseRequest = LøsBrevbestillingDto(
                            behandlingReferanse = request.behandlingReferanse,
                            bestillingReferanse = brevbestillingReferanse,
                            status = BrevbestillingLøsningStatus.AUTOMATISK_FERDIGSTILT,
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
                        call.respond(BrevbestillingResponse(
                            referanse = UUID.fromString(call.pathParameters.get("referanse"))!!,
                            brev = Brev(overskrift = "Overskrift", tekstbolker = emptyList()),
                            opprettet = LocalDateTime.now(),
                            oppdatert = LocalDateTime.now(),
                            behandlingReferanse = UUID.randomUUID(),
                            brevtype = Brevtype.INNVILGELSE,
                            språk = Språk.NB,
                            status = Status.REGISTRERT,
                        ))
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
    internal data class TestToken(
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

        // Tilgang
        System.setProperty("integrasjon.brev.url", "http://localhost:${brev.port()}")
        System.setProperty("integrasjon.brev.scope", "brev")
        System.setProperty("integrasjon.brev.azp", "azp")

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
        System.setProperty("integrasjon.tilgang.azp", "azp")

        // Postmottak
        System.setProperty("integrasjon.postmottak.azp", "azp")

        // Foreldrepenger
        System.setProperty("integrasjon.foreldrepenger.url", "http://localhost:${foreldrepenger.port()}")
        System.setProperty("integrasjon.foreldrepenger.scope", "scope")

        // Sykepenger
        System.setProperty("integrasjon.sykepenger.url", "http://localhost:${sykepenger.port()}")
        System.setProperty("integrasjon.sykepenger.scope", "scope")
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