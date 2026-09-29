package no.nav.aap.behandlingsflyt.test.fakes

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.aap.arenaoppslag.kontrakt.apiv1.HarHistorikkResponse
import no.nav.aap.arenaoppslag.kontrakt.apiv1.ArenaSakOppsummeringKontrakt
import no.nav.aap.arenaoppslag.kontrakt.apiv1.SakerResponse
import no.nav.aap.arenaoppslag.kontrakt.migrering.ArenaSykdomsvurderingResponse
import no.nav.aap.arenaoppslag.kontrakt.migrering.ArenaDiagnose
import no.nav.aap.arenaoppslag.kontrakt.migrering.ArenaVilkar
import no.nav.aap.arenaoppslag.kontrakt.migrering.GjenstaaendeKvote
import no.nav.aap.arenaoppslag.kontrakt.migrering.KravResponse
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

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
                            ArenaSakOppsummeringKontrakt(
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
            get("/api/migrering/{saksnummerArena}/sykdom") {
                call.respond(
                    ArenaSykdomsvurderingResponse(
                        vedtakId = 1,
                        begrunnelse = "Oppfyller vilkårene for 11-5",
                        vilkar = listOf(
                            ArenaVilkar(
                                id = 1,
                                kode = "INNTNEDS",
                                status = "J",
                                begrunnelse = null
                            ),
                            ArenaVilkar(
                                id = 2,
                                kode = "SYKSKADLYT",
                                status = "J",
                                begrunnelse = null
                            ),
                            ArenaVilkar(
                                id = 3,
                                kode = "AAARBEVNE",
                                status = "J",
                                begrunnelse = null
                            ),
                        ),
                        diagnoser = listOf(
                            ArenaDiagnose(
                                kodeverk = "ICD-10",
                                kode = "M797",
                                type = "HOVED",
                                opprettet = LocalDate.of(2016, 1, 1),
                            ),
                            ArenaDiagnose(
                                kodeverk = "ICD-10",
                                kode = "M797",
                                type = "BI",
                                opprettet = LocalDate.of(2016, 1, 1),
                            )
                        )
                    )
                )
            }
            get("/api/migrering/{saksnummer}/krav") {
                call.respond(
                    KravResponse(
                        lopenr = 123456,
                        aar = 2016,
                        soknadsdato = LocalDate.now().minusYears(1),
                        migreringsdato = LocalDate.now().with(TemporalAdjusters.previous(DayOfWeek.MONDAY)),
                        gjenstaaendeKvote = GjenstaaendeKvote(
                            ordinaer = 150
                        )
                    )
                )
            }
        }
    }
}
