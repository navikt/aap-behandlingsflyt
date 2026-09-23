package no.nav.aap.behandlingsflyt.test

import no.nav.aap.behandlingsflyt.arena.ArenaDiagnose
import no.nav.aap.behandlingsflyt.arena.ArenaDiagnoseType
import no.nav.aap.behandlingsflyt.arena.ArenaOppslagGateway
import no.nav.aap.behandlingsflyt.arena.ArenaSakOppsummering
import no.nav.aap.behandlingsflyt.arena.ArenaSakerResponse
import no.nav.aap.behandlingsflyt.arena.ArenaSykdomsvurderingResponse
import no.nav.aap.behandlingsflyt.arena.ArenaVilkar
import no.nav.aap.behandlingsflyt.arena.HarArenaHistorikkResponse
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.komponenter.gateway.Factory
import java.time.LocalDate

class FakeArenaOppslagGateway : ArenaOppslagGateway {
    companion object : Factory<ArenaOppslagGateway> {
        override fun konstruer(): ArenaOppslagGateway {
            return FakeArenaOppslagGateway()
        }
    }

    override fun hentHarHistorikk(ident: Ident): HarArenaHistorikkResponse {
        return HarArenaHistorikkResponse(harHistorikk = false)
    }

    override fun hentSakerForPerson(ident: Ident): ArenaSakerResponse {
        return ArenaSakerResponse(
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
    }

    override fun hentSykdomsvurdering(saksnummerArena: String): ArenaSykdomsvurderingResponse {
        return ArenaSykdomsvurderingResponse(
            begrunnelse = "Oppfyller vilkårene for 11-5",
            vilkar = listOf(
                ArenaVilkar(
                    kode = "INNTNEDS",
                    oppfylt = true
                ),
                ArenaVilkar(
                    kode = "SYKSKADLYT",
                    oppfylt = true
                ),
                ArenaVilkar(
                    kode = "AAARBEVNE",
                    oppfylt = true
                ),
            ),
            diagnoser = listOf(
                ArenaDiagnose(
                    kodeverk = "ICD10",
                    kode = "M797",
                    type = ArenaDiagnoseType.HOVEDDIAGNOSE,
                    opprettet = LocalDate.of(2016, 1, 1),
                ),
                ArenaDiagnose(
                    kodeverk = "ICD10",
                    kode = "M797",
                    type = ArenaDiagnoseType.BIDIAGNOSE,
                    opprettet = LocalDate.of(2016, 1, 1),
                )
            )
        )
    }
}
