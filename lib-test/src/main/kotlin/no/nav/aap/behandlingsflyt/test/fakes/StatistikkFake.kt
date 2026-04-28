package no.nav.aap.behandlingsflyt.test.fakes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.StoppetBehandling

class StatistikkFake : FakeServer() {
    internal val hendelser = mutableListOf<StoppetBehandling>()

    override val server = embeddedServer(Netty, port = 0, module = module())
    override fun start() { server.start() }

    
    private fun module(): Application.() -> Unit = {
        installerContentNegotiation()
        installerStatusPages("STATISTIKK")
        routing {
            post("/stoppetBehandling") {
                val receive = call.receive<StoppetBehandling>()
                hendelser.add(receive)
                call.respond(status = HttpStatusCode.Accepted, message = "{}")
            }
        }
    }
}
