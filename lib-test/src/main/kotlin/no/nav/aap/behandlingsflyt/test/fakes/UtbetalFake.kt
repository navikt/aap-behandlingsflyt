package no.nav.aap.behandlingsflyt.test.fakes

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.aap.utbetal.trekk.TrekkDto
import no.nav.aap.utbetal.trekk.TrekkPosteringDto
import no.nav.aap.utbetal.trekk.TrekkResponsDto
import java.time.LocalDate
import java.util.*

class UtbetalFake : FakeServer() {
    override val server = embeddedServer(Netty, port = 0, module = module())
    override fun start() { server.start() }

    private fun module(): Application.() -> Unit = {
        install(ContentNegotiation) {
            jackson {
                registerModule(JavaTimeModule())
            }
        }
        installerStatusPages("UTBETAL")
        routing {
            post("/tilkjentytelse") {
                call.respond(HttpStatusCode.NoContent)
            }
            get("/trekk/{saksnummer}") {
                val saksnummer = call.parameters["saksnummer"] ?: error("Mangler saksnummer")
                val trekkRespons = TrekkResponsDto(
                    listOf(
                        TrekkDto(
                            saksnummer = saksnummer,
                            behandlingsreferanse = UUID.randomUUID(),
                            dato = LocalDate.now(),
                            beløp = 1234,
                            aktiv = true,
                            posteringer = listOf(
                                TrekkPosteringDto(LocalDate.now(), 400),
                                TrekkPosteringDto(LocalDate.now().plusDays(1), 834)
                            ),
                        ),
                        TrekkDto(
                            saksnummer, UUID.randomUUID(), LocalDate.now().minusDays(1), 200, false, emptyList()
                        )
                    )
                )
                call.respond(trekkRespons)
            }
        }
    }
}
