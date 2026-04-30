package no.nav.aap.behandlingsflyt.test.fakes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

class KabalFake : FakeServer() {
    override val server = embeddedServer(Netty, port = 0, module = module())
    override fun start() { server.start() }

    
    private fun module(): Application.() -> Unit = {
        installerContentNegotiation()
        installerStatusPages("KABAL")
        routing {
            post("/api/oversendelse/v4/sak") {
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}
