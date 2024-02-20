package no.nav.aap.behandlingsflyt.test

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
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.adapter.PERSON_QUERY
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.IDENT_QUERY
import no.nav.aap.pdl.HentPersonBolkResult
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import no.nav.aap.pdl.PdlPerson as BarnPdlPerson
import no.nav.aap.pdl.PdlRelasjonData as BarnPdlData

class Fakes : AutoCloseable {
    private var azure: NettyApplicationEngine =
        embeddedServer(Netty, port = 0, module = Application::azureFake).apply { start() }
    private val pdl = embeddedServer(Netty, port = 0, module = Application::pdlFake).apply { start() }

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
    }

    override fun close() {
        pdl.stop(0L, 0L)
        azure.stop(0L, 0L)
    }

    fun withFødselsdatoFor(ident: String, fødselsdato: LocalDate) {
        fakedFødselsdatoResponsees[ident] = fødselsdato.format(DateTimeFormatter.ISO_LOCAL_DATE)
    }
}

fun NettyApplicationEngine.port(): Int =
    runBlocking { resolvedConnectors() }
        .first { it.type == ConnectorType.HTTP }
        .port

private val fakedFødselsdatoResponsees = mutableMapOf(
    "12345678910" to "1990-01-01"
)

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
                BARN_RELASJON_QUERY -> call.respond(barnRelasjoner())
                PERSON_BOLK_QUERY -> call.respond(barn())
                else -> call.respond(HttpStatusCode.BadRequest)
            }
        }
    }
}

private fun barn() = PdlRelasjonDataResponse(
    errors = null,
    extensions = null,
    data = BarnPdlData(
        hentPersonBolk = listOf(
            HentPersonBolkResult(
                ident = "10123456789",
                person = BarnPdlPerson(
                    foedsel = listOf(PdlFoedsel("2020-01-01"))
                )
            )
        )
    )
)

private fun barnRelasjoner() = PdlRelasjonDataResponse(
    errors = null,
    extensions = null,
    data = BarnPdlData(
        hentPerson = BarnPdlPerson(
            forelderBarnRelasjon = listOf(
                PdlRelasjon(relatertPersonsIdent = "10123456789")
            )
        )
    )
)

private fun identer(req: PdlRequest) = PdlIdenterDataResponse(
    errors = null,
    extensions = null,
    data = PdlIdenterData(
        hentIdenter = PdlIdenter(
            identer = listOf(
                PdlIdent(req.variables.ident ?: "", false, PdlGruppe.FOLKEREGISTERIDENT),
                PdlIdent("12345678911", false, PdlGruppe.NPID),
                PdlIdent("1234567890123", false, PdlGruppe.AKTORID)
            )
        )
    ),
)

private fun personopplysninger(req: PdlRequest) = PdlPersoninfoDataResponse(
    errors = null,
    extensions = null,
    data = PdlPersoninfoData(
        hentPerson = PdlPersoninfo(
            foedselsdato = fakedFødselsdatoResponsees[req.variables.ident] ?: "1990-01-01"
        )
    ),
)

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
