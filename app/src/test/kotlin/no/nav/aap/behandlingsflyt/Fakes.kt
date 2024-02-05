package no.nav.aap.behandlingsflyt

import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.runBlocking
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.PdlConfig
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.PdlData
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.PdlGruppe
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.PdlIdent
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.PdlIdenter
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.PdlRequest
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.PdlResponse
import no.nav.aap.ktor.client.AzureConfig
import java.net.URI

class Fakes : AutoCloseable {
    private val pdl = embeddedServer(Netty, port = 0, module = Application::pdlFake).apply { start() }
    val pdlConf = PdlConfig("", "http://localhost:${pdl.port()}")

    private val azure = embeddedServer(Netty, port = 0, module = Application::azureFake).apply { start() }
    val azureConf = AzureConfig(URI("http://localhost:${azure.port()}/token").toURL(), "", "")

    override fun close() {
        pdl.stop(0L, 0L)
        azure.stop(0L, 0L)
    }
}

fun NettyApplicationEngine.port(): Int =
    runBlocking { resolvedConnectors() }
        .first { it.type == ConnectorType.HTTP }
        .port

fun Application.pdlFake() {
    install(ContentNegotiation) {
        jackson()
    }
    routing {
        post {
            val req = call.receive<PdlRequest>()
            call.respond(
                PdlResponse(
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
            )
        }
    }
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
    val exprires_in: Int = 3599,
    val access_token: String = "very.secure.token"
)
