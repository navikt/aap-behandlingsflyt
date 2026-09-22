package no.nav.aap.behandlingsflyt.test.fakes

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.aap.behandlingsflyt.arena.ArenaDiagnose
import no.nav.aap.behandlingsflyt.arena.HarArenaHistorikkResponse
import no.nav.aap.behandlingsflyt.arena.ArenaSakOppsummering
import no.nav.aap.behandlingsflyt.arena.ArenaSakerResponse
import no.nav.aap.behandlingsflyt.arena.ArenaSykdomsvurderingResponse
import java.time.LocalDate

class ArenaoppslagFake : FakeServer() {
    override val server = embeddedServer(Netty, port = 0, module = module())
    override fun start() { server.start() }

    private fun module(): Application.() -> Unit = {
        installerContentNegotiation()
        installerStatusPages("ARENAOPPSLAG")
        routing {
            post("/api/v1/person/historikk") {
                call.respond(HarArenaHistorikkResponse(harHistorikk = false))
            }
            post("/api/v1/person/saker") {
                call.respond(
                    ArenaSakerResponse(
                        saker = listOf(
                            ArenaSakOppsummering(
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
            post("/api/v1/migrering/sykdom") {
                call.respond(
                    ArenaSykdomsvurderingResponse(
                        begrunnelse = "Oppfyller vilkårene for 11-5",
                        ordinærAAP = true,
                        diagnose = ArenaDiagnose(
                            kodeverk = "ICD10",
                            hoveddiagnose = listOf("M797")
                        )
                    )
                )
            }
        }
    }
}
