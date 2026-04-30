package no.nav.aap.behandlingsflyt.test.fakes

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory

class DatadelingFake : FakeServer() {
    private val logger = LoggerFactory.getLogger(javaClass)

    override val server = embeddedServer(Netty, port = 0, module = module())
    override fun start() { server.start() }

    
    private fun module(): Application.() -> Unit = {
        installerContentNegotiation()
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
}
