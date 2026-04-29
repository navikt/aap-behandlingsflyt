package no.nav.aap.behandlingsflyt.test.fakes

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.aap.behandlingsflyt.behandling.gosysoppgave.OpprettOppgaveRequest
import no.nav.aap.behandlingsflyt.behandling.gosysoppgave.OpprettOppgaveResponse

class GosysFake : FakeServer() {
    override val server = embeddedServer(Netty, port = 0, module = module())
    override fun start() {
        server.start()
    }

    private fun module(): Application.() -> Unit = {
        installerContentNegotiation()
        installerStatusPages("GOSYS")
        routing {
            route("/api/v1/oppgaver") {
                post {
                    call.receive<OpprettOppgaveRequest>()
                    call.respond(
                        OpprettOppgaveResponse(
                            success = true
                        )
                    )
                }
            }
        }
    }
}
