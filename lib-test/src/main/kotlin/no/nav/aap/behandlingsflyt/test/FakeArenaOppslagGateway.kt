package no.nav.aap.behandlingsflyt.test

import no.nav.aap.behandlingsflyt.arena.ArenaOppslagGateway
import no.nav.aap.arenaoppslag.kontrakt.apiv1.ArenaSakOppsummeringKontrakt
import no.nav.aap.arenaoppslag.kontrakt.apiv1.SakerResponse
import no.nav.aap.arenaoppslag.kontrakt.migrering.GjenstaaendeKvote
import no.nav.aap.arenaoppslag.kontrakt.apiv1.HarHistorikkResponse
import no.nav.aap.arenaoppslag.kontrakt.migrering.ArenaRefusjonskravResponse
import no.nav.aap.arenaoppslag.kontrakt.migrering.ArenaSykdomsvurderingResponse
import no.nav.aap.arenaoppslag.kontrakt.migrering.ArenaVilkar as ArenaVilkarKontrakt
import no.nav.aap.arenaoppslag.kontrakt.migrering.ArenaDiagnose as ArenaDiagnoseKontrakt
import no.nav.aap.arenaoppslag.kontrakt.migrering.KravResponse
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.komponenter.gateway.Factory
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

class FakeArenaOppslagGateway : ArenaOppslagGateway {
    companion object : Factory<ArenaOppslagGateway> {
        override fun konstruer(): ArenaOppslagGateway {
            return FakeArenaOppslagGateway()
        }
    }

    override fun hentHarHistorikk(ident: Ident): HarHistorikkResponse {
        return HarHistorikkResponse(harHistorikk = false)
    }

    override fun hentSakerForPerson(ident: Ident): SakerResponse {
        return SakerResponse(
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
    }


    override fun hentKravDataForSak(arenasaksnummer: String): KravResponse {
        return KravResponse(
            lopenr = 123456,
            aar = 2016,
            soknadsdato = LocalDate.now().minusYears(1),
            migreringsdato = LocalDate.now().with(TemporalAdjusters.previous(DayOfWeek.MONDAY)),
            gjenstaaendeKvote = GjenstaaendeKvote(
                ordinaer = 150
            )
        )
    }

    override fun hentRefusjonskrav(saksnummerArena: String): ArenaRefusjonskravResponse {
        return ArenaRefusjonskravResponse(refusjonskrav = null)
    }

    override fun hentSykdomsvurdering(saksnummerArena: String): ArenaSykdomsvurderingResponse {
        return ArenaSykdomsvurderingResponse(
            vedtakId = 1,
            begrunnelse = "Oppfyller vilkårene for 11-5",
            vilkar = listOf(
                ArenaVilkarKontrakt(
                    id = 1,
                    kode = "INNTNEDS",
                    status = "J",
                    begrunnelse = null
                ),
                ArenaVilkarKontrakt(
                    id = 2,
                    kode = "SYKSKADLYT",
                    status = "J",
                    begrunnelse = null
                ),
                ArenaVilkarKontrakt(
                    id = 3,
                    kode = "AAARBEVNE",
                    status = "J",
                    begrunnelse = null
                ),
            ),
            diagnoser = listOf(
                ArenaDiagnoseKontrakt(
                    kodeverk = "ICD-10",
                    kode = "M797",
                    type = "HOVED",
                    opprettet = LocalDate.of(2016, 1, 1),
                ),
                ArenaDiagnoseKontrakt(
                    kodeverk = "ICD-10",
                    kode = "M797",
                    type = "BI",
                    opprettet = LocalDate.of(2016, 1, 1),
                )
            )
        )
    }
}
