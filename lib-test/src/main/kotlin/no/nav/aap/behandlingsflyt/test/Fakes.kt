package no.nav.aap.behandlingsflyt.test

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.runBlocking
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.adapter.BARN_RELASJON_QUERY
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.adapter.PERSON_BOLK_QUERY
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.adapter.PERSON_QUERY
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.IDENT_QUERY
import no.nav.aap.behandlingsflyt.test.modell.TestPerson
import no.nav.aap.pdl.HentPersonBolkResult
import no.nav.aap.pdl.PDLDødsfall
import no.nav.aap.pdl.PdlFoedsel
import no.nav.aap.pdl.PdlGruppe
import no.nav.aap.pdl.PdlIdent
import no.nav.aap.pdl.PdlIdenter
import no.nav.aap.pdl.PdlIdenterData
import no.nav.aap.pdl.PdlIdenterDataResponse
import no.nav.aap.pdl.PdlPersoninfo
import no.nav.aap.pdl.PdlPersoninfoData
import no.nav.aap.pdl.PdlPersoninfoDataResponse
import no.nav.aap.pdl.PdlRelasjon
import no.nav.aap.pdl.PdlRelasjonDataResponse
import no.nav.aap.pdl.PdlRequest
import no.nav.aap.verdityper.sakogbehandling.Ident
import no.nav.aap.yrkesskade.YrkesskadeModell
import no.nav.aap.yrkesskade.YrkesskadeRequest
import no.nav.aap.yrkesskade.Yrkesskader
import java.time.LocalDate
import no.nav.aap.pdl.PdlPerson as BarnPdlPerson
import no.nav.aap.pdl.PdlRelasjonData as BarnPdlData

class Fakes : AutoCloseable {
    private var azure: NettyApplicationEngine =
        embeddedServer(Netty, port = 0, module = Application::azureFake).apply { start() }
    private val pdl = embeddedServer(Netty, port = 0, module = Application::pdlFake).apply { start() }
    private val yrkesskade = embeddedServer(Netty, port = 0, module = Application::yrkesskadeFake).apply { start() }

    init {
        // Azure
        System.setProperty("azure.openid.config.token.endpoint", "http://localhost:${azure.port()}/token")
        System.setProperty("azure.app.client.id", "")
        System.setProperty("azure.app.client.secret", "")
        System.setProperty("azure.openid.config.jwks.uri", "")
        System.setProperty("azure.openid.config.issuer", "")

        // Pdl
        System.setProperty("integrasjon.pdl.url", "http://localhost:${pdl.port()}")
        System.setProperty("integrasjon.pdl.scope", "pdl")

        // Yrkesskade
        System.setProperty("integrasjon.yrkesskade.url", "http://localhost:${yrkesskade.port()}")
        System.setProperty("integrasjon.yrkesskade.scope", "yrkesskade")


        // Legg til alle testpersoner
        listOf(PERSON_MED_BARN_65ÅR).forEach { leggTil(it) }
    }

    override fun close() {
        yrkesskade.stop(0L, 0L)
        pdl.stop(0L, 0L)
        azure.stop(0L, 0L)
    }

    fun leggTil(person: TestPerson) {
        person.barn.forEach { leggTil(it) }
        fakePersoner[person.aktivIdent()] = person
        if(person.yrkesskade){
            returnerYrkesskade(person.aktivIdent())
        }
    }

    fun returnerYrkesskade(ident: String) {
        returnerYrkesskade.add(ident)
    }
}

fun NettyApplicationEngine.port(): Int =
    runBlocking { resolvedConnectors() }
        .first { it.type == ConnectorType.HTTP }
        .port

val BARNLØS_PERSON_30ÅR =
    TestPerson(setOf(Ident("12345678910", true)), fødselsdato = Fødselsdato(LocalDate.now().minusYears(30)))
val BARNLØS_PERSON_18ÅR =
    TestPerson(
        setOf(Ident("42346734567", true)),
        fødselsdato = Fødselsdato(LocalDate.now().minusYears(18).minusDays(10))
    )
val PERSON_MED_BARN_65ÅR =
    TestPerson(
        setOf(Ident("86322434234", true)), fødselsdato = Fødselsdato(LocalDate.now().minusYears(65)), barn = listOf(
            BARNLØS_PERSON_18ÅR, BARNLØS_PERSON_30ÅR
        )
    )

private val fakePersoner: MutableMap<String, TestPerson> = mutableMapOf()

fun Application.pdlFake() {
    install(ContentNegotiation) {
        jackson()
    }
    routing {
        post {
            val req = call.receive<PdlRequest>()

            when (req.query) {
                IDENT_QUERY -> call.respond(identer(req))
                PERSON_QUERY -> call.respond(personopplysninger(req))
                BARN_RELASJON_QUERY -> call.respond(barnRelasjoner(req))
                PERSON_BOLK_QUERY -> call.respond(barn(req))
                else -> call.respond(HttpStatusCode.BadRequest)
            }
        }
    }
}


private val returnerYrkesskade = mutableListOf<String>()

fun Application.yrkesskadeFake() {
    install(ContentNegotiation) {
        jackson {
            registerModule(JavaTimeModule())
        }
    }
    routing {
        post("api/v1/saker/oprett"){
            val req = call.receive<Ident>()
            returnerYrkesskade.add(req.identifikator)
            call.respond(req)
        }
        post("/api/v1/saker/") {
            val req = call.receive<YrkesskadeRequest>()

            if (req.foedselsnumre.first() in returnerYrkesskade) {
                call.respond(
                    Yrkesskader(
                        skader = listOf(
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
                                skadedato = LocalDate.now(),
                                kildetabell = "Yrkesskade",
                                kildesystem = "Yrkesskade",
                                saksreferanse = "1234"
                            )
                        )
                    )
                )
            } else {
                call.respond(Yrkesskader(skader = listOf()))
            }
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
        ident = person.aktivIdent(),
        person = BarnPdlPerson(
            foedsel = listOf(PdlFoedsel(person.fødselsdato.toFormatedString())),
            doedsfall = mapDødsfall(person)
        )
    )
}

fun mapDødsfall(person: TestPerson): Set<PDLDødsfall>? {
    if (person.dødsdato == null) {
        return null
    }
    return setOf(PDLDødsfall(person.dødsdato.toFormatedString()))
}

private fun barnRelasjoner(req: PdlRequest): PdlRelasjonDataResponse {
    val testPerson = hentEllerGenererTestPerson(req)
    return PdlRelasjonDataResponse(
        errors = null,
        extensions = null,
        data = BarnPdlData(
            hentPerson = BarnPdlPerson(
                forelderBarnRelasjon = testPerson.barn
                    .map { PdlRelasjon(it.aktivIdent()) }
                    .toList()
            )
        )
    )
}

private fun identer(req: PdlRequest): PdlIdenterDataResponse {
    val person = hentEllerGenererTestPerson(req)

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

private fun hentEllerGenererTestPerson(req: PdlRequest): TestPerson {
    val forespurtIdent = req.variables.ident ?: ""
    val person = fakePersoner[forespurtIdent]
    if (person != null) {
        return person
    }

    return TestPerson(setOf(Ident(forespurtIdent)), fødselsdato = Fødselsdato(LocalDate.now().minusYears(30)))
}

fun mapIdent(person: TestPerson?): List<PdlIdent> {
    if (person == null) {
        return emptyList()
    }
    return listOf(PdlIdent(person.aktivIdent(), false, PdlGruppe.FOLKEREGISTERIDENT))
}

private fun personopplysninger(req: PdlRequest): PdlPersoninfoDataResponse {
    val testPerson = hentEllerGenererTestPerson(req)
    return PdlPersoninfoDataResponse(
        errors = null,
        extensions = null,
        data = PdlPersoninfoData(
            hentPerson = mapPerson(testPerson)
        ),
    )
}

fun mapPerson(person: TestPerson?): PdlPersoninfo? {
    if (person == null) {
        return null
    }
    return PdlPersoninfo(person.fødselsdato.toFormatedString())
}

fun Application.azureFake() {
    install(ContentNegotiation) {
        jackson()
    }
    routing {
        post("/token") {
            call.respond(TestToken())
        }
    }
}

internal data class TestToken(
    val access_token: String = "very.secure.token",
    val refresh_token: String = "very.secure.token",
    val id_token: String = "very.secure.token",
    val token_type: String = "token-type",
    val scope: String? = null,
    val expires_in: Int = 3599,
)
