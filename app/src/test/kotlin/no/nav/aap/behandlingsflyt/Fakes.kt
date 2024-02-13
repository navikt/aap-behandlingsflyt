package no.nav.aap.behandlingsflyt

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
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.adapter.PERSON_QUERY
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.IDENT_QUERY
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.PdlData
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.PdlGruppe
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.PdlIdent
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.PdlIdenter
import no.nav.aap.ktor.client.auth.azure.AzureConfig
import no.nav.aap.pdl.PdlConfig
import no.nav.aap.pdl.PdlRequest
import no.nav.aap.pdl.PdlResponse
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class Fakes : AutoCloseable {
    private val pdl = embeddedServer(Netty, port = 0, module = Application::pdlFake).apply { start() }
    val pdlConf = PdlConfig("", "http://localhost:${pdl.port()}")

    private val azure = embeddedServer(Netty, port = 0, module = Application::azureFake).apply { start() }
    val azureConf = AzureConfig(
        tokenEndpoint = "http://localhost:${azure.port()}/token",
        clientId = "",
        clientSecret = "",
        jwksUri = "",
        issuer = ""
    )

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
                else -> call.respond(HttpStatusCode.BadRequest)
            }
        }
    }
}

private fun identer(req: PdlRequest) = PdlResponse(
    errors = null,
    extensions = null,
    data = PdlData(
        hentIdenter = PdlIdenter(
            identer = listOf(
                PdlIdent(req.variables.ident, false, PdlGruppe.FOLKEREGISTERIDENT),
                PdlIdent("12345678911", false, PdlGruppe.NPID),
                PdlIdent("1234567890123", false, PdlGruppe.AKTORID)
            )
        )
    ),
)

private fun personopplysninger(req: PdlRequest) = PdlResponse(
    errors = null,
    extensions = null,
    data = no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.adapter.PdlData(
        hentPerson = no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.adapter.PdlPerson(
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
    val exprires_in: Int = 3599,
    val access_token: String = "very.secure.token"
)
