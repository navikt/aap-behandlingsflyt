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
import no.nav.aap.Inntekt.InntektRequest
import no.nav.aap.Inntekt.InntektResponse
import no.nav.aap.Inntekt.SumPi
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.adapter.BARN_RELASJON_QUERY
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.adapter.PERSON_BOLK_QUERY
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektPerÅr
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.adapter.PERSON_QUERY
import no.nav.aap.behandlingsflyt.hendelse.statistikk.StatistikkHendelseDTO
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.IDENT_QUERY
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.PdlPersoninfoGateway.PERSONINFO_QUERY
import no.nav.aap.behandlingsflyt.test.modell.TestPerson
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
import no.nav.aap.pdl.PdlRelasjonDataResponse
import no.nav.aap.pdl.PdlRequest
import no.nav.aap.verdityper.Beløp
import no.nav.aap.verdityper.sakogbehandling.Ident
import no.nav.aap.yrkesskade.YrkesskadeModell
import no.nav.aap.yrkesskade.YrkesskadeRequest
import no.nav.aap.yrkesskade.Yrkesskader
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.time.LocalDate
import java.time.Year
import java.util.*
import no.nav.aap.pdl.PdlRelasjonData as BarnPdlData

class Fakes(azurePort: Int = 0) : AutoCloseable {
    private val log: Logger = LoggerFactory.getLogger(Fakes::class.java)
    private val azure = embeddedServer(Netty, port = azurePort, module = { azureFake() }).start()
    private val pdl = embeddedServer(Netty, port = 0, module = { pdlFake() }).start()
    private val yrkesskade = embeddedServer(Netty, port = 0, module = { yrkesskadeFake() }).start()
    private val inntekt = embeddedServer(Netty, port = 0, module = { poppFake() }).start()
    private val oppgavestyring = embeddedServer(Netty, port = 0, module = { oppgavestyringFake() }).start()
    private val fakePersoner: MutableMap<String, TestPerson> = mutableMapOf()
    private val saf = embeddedServer(Netty, port = 0, module = { safFake() }).apply { start() }

    private val statikk = embeddedServer(Netty, port = 0, module = { statistikkFake() }).apply { start() }
    val statistikkHendelser = mutableListOf<StatistikkHendelseDTO>()


    init {
        Thread.currentThread().setUncaughtExceptionHandler { _, e -> log.error("Uhåndtert feil", e) }
        // Azure
        System.setProperty("azure.openid.config.token.endpoint", "http://localhost:${azure.port()}/token")
        System.setProperty("azure.app.client.id", "behandlingsflyt")
        System.setProperty("azure.app.client.secret", "")
        System.setProperty("azure.openid.config.jwks.uri", "http://localhost:${azure.port()}/jwks")
        System.setProperty("azure.openid.config.issuer", "behandlingsflyt")

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
        System.setProperty("integrasjon.medl.url", "https://medlemskap-medl-api.dev-fss-pub.nais.io/api/v1/medlemskapsunntak")
        System.setProperty("integrasjon.medl.scope", "medl")

        // Statistikk-app
        System.setProperty("integrasjon.statistikk.url", "http://localhost:${statikk.port()}")

        // testpersoner
        val BARNLØS_PERSON_30ÅR =
            TestPerson(
                identer = setOf(Ident("12345678910", true)),
                fødselsdato = Fødselsdato(
                    LocalDate.now().minusYears(30),
                ),
                inntekter = (1..10).map { InntektPerÅr(Year.now().minusYears(it.toLong()), Beløp("1000000.0")) }
            )
        val BARNLØS_PERSON_18ÅR =
            TestPerson(
                identer = setOf(Ident("42346734567", true)),
                fødselsdato = Fødselsdato(LocalDate.now().minusYears(18).minusDays(10)),
                inntekter = (1..10).map { InntektPerÅr(Year.now().minusYears(it.toLong()), Beløp("1000000.0")) }
            )
        val PERSON_MED_BARN_65ÅR =
            TestPerson(
                identer = setOf(Ident("86322434234", true)),
                fødselsdato = Fødselsdato(LocalDate.now().minusYears(65)),
                barn = listOf(
                    BARNLØS_PERSON_18ÅR, BARNLØS_PERSON_30ÅR
                ),
                inntekter = (1..10).map { InntektPerÅr(Year.now().minusYears(it.toLong()), Beløp("1000000.0")) }
            )

        // Legg til alle testpersoner
        listOf(PERSON_MED_BARN_65ÅR).forEach { leggTil(it) }
    }

    override fun close() {
        yrkesskade.stop(0L, 0L)
        pdl.stop(0L, 0L)
        azure.stop(0L, 0L)
        inntekt.stop(0L, 0L)
    }

    fun leggTil(person: TestPerson) {
        person.identer.forEach { fakePersoner[it.identifikator] = person }
        person.barn.forEach { leggTil(it) }
    }

    fun returnerYrkesskade(ident: String) {
        returnerYrkesskade.add(ident)
    }


    private fun NettyApplicationEngine.port(): Int =
        runBlocking { resolvedConnectors() }
            .first { it.type == ConnectorType.HTTP }
            .port

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
                call.respond(status = HttpStatusCode.InternalServerError, message = ErrorRespons(cause.message))
            }
        }
        routing {
            post("/behandling") {
                call.respond(HttpStatusCode.NoContent)
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
                call.respond(status = HttpStatusCode.InternalServerError, message = ErrorRespons(cause.message))
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
                call.respond(status = HttpStatusCode.InternalServerError, message = ErrorRespons(cause.message))
            }
        }
        routing {
            post {
                val req = call.receive<PdlRequest>()

                when (req.query) {
                    IDENT_QUERY -> call.respond(identer(req))
                    PERSON_QUERY -> call.respond(personopplysninger(req))
                    PERSONINFO_QUERY -> call.respond(navn(req))
                    BARN_RELASJON_QUERY -> call.respond(barnRelasjoner(req))
                    PERSON_BOLK_QUERY -> call.respond(barn(req))
                    else -> call.respond(HttpStatusCode.BadRequest)
                }
            }
        }
    }

    private fun Application.statistikkFake() {
        install(ContentNegotiation) {
            jackson()
        }
        routing {
            post("/motta") {
                val receive = call.receive<StatistikkHendelseDTO>()
                statistikkHendelser.add(receive)
                call.respond(HttpStatusCode.OK)
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
                    ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, "ktor_logo.pdf")
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
                    call.respondText(
                        """
                            {
                              "data": {
                                "dokumentoversiktFagsak": {
                                  "journalposter": [
                                    {
                                      "journalpostId": "453876204",
                                      "behandlingstema": null,
                                      "antallRetur": null,
                                      "kanal": "NAV_NO",
                                      "innsynsregelBeskrivelse": "Standardreglene avgjør om dokumentet vises",
                                      "sak": {
                                        "datoOpprettet": "2024-06-20T11:01:14",
                                        "fagsakId": "4LE7Uo0",
                                        "fagsaksystem": "KELVIN",
                                        "sakstype": "FAGSAK",
                                        "tema": "AAP"
                                      },
                                      "dokumenter": [
                                        {
                                          "dokumentInfoId": "454271758",
                                          "tittel": "Søknad om Arbeidsavklaringspenger",
                                          "brevkode": "NAV 11-13.05",
                                          "Dokumentstatus": null,
                                          "datoFerdigstilt": null,
                                          "originalJournalpostId": "453876204",
                                          "skjerming": null,
                                          "logiskeVedlegg": [],
                                          "dokumentvarianter": [
                                            {
                                              "variantformat": "ARKIV",
                                              "filnavn": null,
                                              "saksbehandlerHarTilgang": false,
                                              "skjerming": null
                                            },
                                            {
                                              "variantformat": "ORIGINAL",
                                              "filnavn": null,
                                              "saksbehandlerHarTilgang": false,
                                              "skjerming": null
                                            }
                                          ]
                                        }
                                      ]
                                    },
                                    {
                                      "journalpostId": "453871794",
                                      "behandlingstema": null,
                                      "antallRetur": null,
                                      "kanal": "NAV_NO",
                                      "innsynsregelBeskrivelse": "Standardreglene avgjør om dokumentet vises",
                                      "sak": {
                                        "datoOpprettet": "2024-05-16T14:46:27",
                                        "fagsakId": "4LE7Uo0",
                                        "fagsaksystem": "KELVIN",
                                        "sakstype": "FAGSAK",
                                        "tema": "AAP"
                                      },
                                      "dokumenter": [
                                        {
                                          "dokumentInfoId": "454266758",
                                          "tittel": "Søknad om Arbeidsavklaringspenger",
                                          "brevkode": "NAV 11-13.05",
                                          "dokumentstatus": null,
                                          "datoFerdigstilt": null,
                                          "originalJournalpostId": "453871794",
                                          "skjerming": null,
                                          "logiskeVedlegg": [],
                                          "dokumentvarianter": [
                                            {
                                              "variantformat": "ARKIV",
                                              "filnavn": null,
                                              "saksbehandlerHarTilgang": false,
                                              "skjerming": null
                                            },
                                            {
                                              "variantformat": "ORIGINAL",
                                              "filnavn": null,
                                              "saksbehandlerHarTilgang": false,
                                              "skjerming": null
                                            }
                                          ]
                                        }
                                      ]
                                    },
                                    {
                                      "journalpostId": "453870298",
                                      "behandlingstema": null,
                                      "antallRetur": null,
                                      "kanal": "NAV_NO",
                                      "innsynsregelBeskrivelse": "Standardreglene avgjør om dokumentet vises",
                                      "sak": {
                                        "datoOpprettet": "2024-05-03T14:45:57",
                                        "fagsakId": "4LE7Uo0",
                                        "fagsaksystem": "KELVIN",
                                        "sakstype": "FAGSAK",
                                        "tema": "AAP"
                                      },
                                      "dokumenter": [
                                        {
                                          "dokumentInfoId": "454265122",
                                          "tittel": "Søknad om Arbeidsavklaringspenger",
                                          "brevkode": "NAV 11-13.05",
                                          "dokumentstatus": null,
                                          "datoFerdigstilt": null,
                                          "originalJournalpostId": "453870298",
                                          "skjerming": null,
                                          "logiskeVedlegg": [],
                                          "dokumentvarianter": [
                                            {
                                              "variantformat": "ARKIV",
                                              "filnavn": null,
                                              "saksbehandlerHarTilgang": false,
                                              "skjerming": null
                                            },
                                            {
                                              "variantformat": "ORIGINAL",
                                              "filnavn": null,
                                              "saksbehandlerHarTilgang": false,
                                              "skjerming": null
                                            }
                                          ]
                                        }
                                      ]
                                    }
                                  ],
                                  "sideInfo": {
                                    "sluttpeker": "NDUzODcwMjk4",
                                    "finnesNesteSide": false,
                                    "antall": 3,
                                    "totaltAntall": 3
                                  }
                                }
                              }
                            }
                """.trimIndent(),
                        contentType = ContentType.Application.Json
                    )
                } else {
                    print("FEIL KALL")
                    call.respond(HttpStatusCode.BadRequest)
                }
            }
        }
    }


    private val returnerYrkesskade = mutableListOf<String>()

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
                call.respond(status = HttpStatusCode.InternalServerError, message = ErrorRespons(cause.message))
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
            data = BarnPdlData(
                hentPersonBolk = barnIdenter
            )
        )
    }

    private fun mapIdentBolk(it: String): HentPersonBolkResult? {
        val person = fakePersoner[it]
        if (person == null) {
            return null
        }
        return HentPersonBolkResult(
            ident = person.identer.first().identifikator,
            person = PdlPersoninfo(
                foedsel = listOf(PdlFoedsel(person.fødselsdato.toFormatedString())),
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
            data = BarnPdlData(
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
        val person = fakePersoner[forespurtIdent]
        if (person == null) {
            fakePersoner[forespurtIdent] = TestPerson(
                identer = setOf(Ident(forespurtIdent)),
                fødselsdato = Fødselsdato(LocalDate.now().minusYears(30)),
                inntekter = (1..10).map { InntektPerÅr(Year.now().minusYears(it.toLong()), Beløp("1000000.0")) }
            )
        }

        return fakePersoner[forespurtIdent]!!
    }

    private fun mapIdent(person: TestPerson?): List<PdlIdent> {
        if (person == null) {
            return emptyList()
        }
        return listOf(PdlIdent(person.identer.first().identifikator, false, PdlGruppe.FOLKEREGISTERIDENT))
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

    private fun mapPerson(person: TestPerson?): PdlPersoninfo? {
        if (person == null) {
            return null
        }
        return PdlPersoninfo(foedsel = listOf(PdlFoedsel(person.fødselsdato.toFormatedString())))
    }

    private fun Application.azureFake() {
        install(ContentNegotiation) {
            jackson()
        }
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                this@azureFake.log.info("AZURE :: Ukjent feil ved kall til '{}'", call.request.local.uri, cause)
                call.respond(status = HttpStatusCode.InternalServerError, message = ErrorRespons(cause.message))
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


    internal data class TestToken(
        val access_token: String,
        val refresh_token: String = "very.secure.token",
        val id_token: String = "very.secure.token",
        val token_type: String = "token-type",
        val scope: String? = null,
        val expires_in: Int = 3599,
    )
}

fun genererIdent(fødselsdato: LocalDate): Ident {
    return Ident(FødselsnummerGenerator.Builder().fodselsdato(fødselsdato).buildAndGenerate())
}
