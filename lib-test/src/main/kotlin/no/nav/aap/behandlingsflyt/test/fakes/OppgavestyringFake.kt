package no.nav.aap.behandlingsflyt.test.fakes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.BehandlingFlytStoppetHendelse
import no.nav.aap.oppgave.enhet.OppgaveEnhetResponse
import org.slf4j.LoggerFactory

class OppgavestyringFake : FakeServer() {
    private val logger = LoggerFactory.getLogger(javaClass)

    override val server = embeddedServer(Netty, port = 0, module = module())
    override fun start() { server.start() }

    private fun module(): Application.() -> Unit = {
        installerContentNegotiation()
        installerStatusPages("OPPGAVE")
        routing {
            post("/oppdater-oppgaver") {
                val received = call.receive<BehandlingFlytStoppetHendelse>()
                val åpneBehov = received.avklaringsbehov.filter { it.status.erÅpent() }
                    .map { Pair(it.avklaringsbehovDefinisjon.name, it.status) }
                logger.info("Åpne behov $åpneBehov")
                logger.info("Fikk oppgave-oppdatering: $received")
                call.respond(HttpStatusCode.NoContent)
            }
            get("/{referanse}/hent-oppgave-enhet") {
                call.respond(OppgaveEnhetResponse(emptyList()))
            }
        }
    }
}
