package no.nav.aap.behandlingsflyt.test.fakes

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.receive
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory

class DatadelingFake : FakeServer() {
    private val logger = LoggerFactory.getLogger(javaClass)

    override val server = embeddedServer(Netty, port = 0, module = module())
    override fun start() {
        server.start()
    }

    private fun module(): Application.() -> Unit = {
        installerContentNegotiation()

        routing {
            post("/api/insert/meldeperioder") {
                loggPayload()
                call.respond("datadeling response")
            }
            post("/api/insert/sakStatus") {
                loggPayload()
                call.respond("datadeling response")
            }
            post("api/insert/vedtak") {
                loggPayload()
                call.respond("datadeling response")
            }
            post("/api/insert/ny-soknad") {
                loggPayload()
                call.respond("datadeling response")
            }
        }
    }

    private suspend fun RoutingContext.loggPayload() {
        val payload = call.receive<String>()
        logger.info("Vedtak: $payload")
    }

}
