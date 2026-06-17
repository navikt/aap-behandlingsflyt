package no.nav.aap.behandlingsflyt.test.fakes

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
import no.nav.aap.meldekort.kontrakt.sak.MeldeperioderV0

class MeldekortFake : FakeServer() {
    override val server = embeddedServer(Netty, port = 0, module = module())
    override fun start() { server.start() }

    private fun module(): Application.() -> Unit = {
        install(ContentNegotiation) {
            jackson {
                registerModule(JavaTimeModule())
            }
        }
        installerStatusPages("MELDEKORT")
        routing {
            post("/api/behandlingsflyt/sak/meldeperioder") {
                call.receive<MeldeperioderV0>()
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}
