package no.nav.aap.behandlingsflyt.test

import no.nav.aap.behandlingsflyt.arena.ArenaOppslagGateway
import no.nav.aap.behandlingsflyt.arena.SakOppsummering
import no.nav.aap.behandlingsflyt.arena.HarHistorikkResponse
import no.nav.aap.behandlingsflyt.arena.SakerResponse
import no.nav.aap.komponenter.gateway.Factory
import java.time.LocalDate

class FakeArenaOppslagGateway : ArenaOppslagGateway {
    companion object : Factory<ArenaOppslagGateway> {
        override fun konstruer(): ArenaOppslagGateway {
            return FakeArenaOppslagGateway()
        }
    }

    override fun hentHarHistorikk(personidentifikator: String): HarHistorikkResponse {
        return HarHistorikkResponse(harHistorikk = false)
    }

    override fun hentSakerForPerson(personidentifikator: String): SakerResponse {
        return SakerResponse(
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
    }
}
