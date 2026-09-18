package no.nav.aap.behandlingsflyt.test.fakes

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.aap.behandlingsflyt.arena.HarHistorikkResponse
import no.nav.aap.behandlingsflyt.arena.SakOppsummering
import no.nav.aap.behandlingsflyt.arena.SakerResponse
import java.time.LocalDate

class ArenaoppslagFake : FakeServer() {
    override val server = embeddedServer(Netty, port = 0, module = module())
    override fun start() { server.start() }

    private fun module(): Application.() -> Unit = {
        installerContentNegotiation()
        installerStatusPages("ARENAOPPSLAG")
        routing {
            post("/api/v1/person/historikk") {
                call.respond(HarHistorikkResponse(harHistorikk = false))
            }
            post("/api/v1/person/saker") {
                call.respond(
                    SakerResponse(
                        saker = listOf(
                            SakOppsummering(
                                sakId = "2016-123456",
                                lopenummer = 123456,
                                aar = 2016,
                                antallVedtak = 1,
                                statuskode = "AKTIV",
                                statusnavn = "Aktiv",
                                sakstype = null,
                                regDato = LocalDate.of(2016, 1, 1),
                                avsluttetDato = null,
                            )
                        )
                    )
                )
            }
        }
    }
}
