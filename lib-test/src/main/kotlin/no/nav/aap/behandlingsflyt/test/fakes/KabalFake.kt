package no.nav.aap.behandlingsflyt.test.fakes

import com.fasterxml.jackson.databind.JsonNode
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.receive
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.concurrent.ConcurrentHashMap

class KabalFake : FakeServer() {
    val oversendteHjemler = ConcurrentHashMap<String, List<String>>()

    override val server = embeddedServer(Netty, port = 0, module = module())
    override fun start() { server.start() }

    
    private fun module(): Application.() -> Unit = {
        installerContentNegotiation()
        installerStatusPages("KABAL")
        routing {
            post("/api/oversendelse/v4/sak") {
                val oversendelse = call.receive<JsonNode>()
                oversendteHjemler[oversendelse.required("kildeReferanse").asText()] =
                    oversendelse.required("hjemler").map { it.asText() }
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}
