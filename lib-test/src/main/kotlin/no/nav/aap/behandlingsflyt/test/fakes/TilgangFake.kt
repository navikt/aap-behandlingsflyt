package no.nav.aap.behandlingsflyt.test.fakes

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.aap.tilgang.BehandlingTilgangRequest
import no.nav.aap.tilgang.JournalpostTilgangRequest
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.SakTilgangRequest
import no.nav.aap.tilgang.TilgangResponse

class TilgangFake : FakeServer() {
    override val server = embeddedServer(Netty, port = 0, module = module())
    override fun start() { server.start() }

    private fun module(): Application.() -> Unit = {
        installerContentNegotiation()
        installerStatusPages("TILGANG")
        routing {
            post("/tilgang/sak") {
                call.receive<SakTilgangRequest>()
                call.respond(TilgangResponse(true))
            }
            post("/tilgang/behandling") {
                call.receive<BehandlingTilgangRequest>()
                call.respond(
                    TilgangResponse(
                        true,
                        tilgangIKontekst = mapOf(Operasjon.SAKSBEHANDLE to true)
                    )
                )
            }
            post("/tilgang/journalpost") {
                call.receive<JournalpostTilgangRequest>()
                call.respond(TilgangResponse(true))
            }
        }
    }
}
