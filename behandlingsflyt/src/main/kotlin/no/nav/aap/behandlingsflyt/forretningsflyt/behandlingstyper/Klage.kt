package no.nav.aap.behandlingsflyt.forretningsflyt.behandlingstyper

import no.nav.aap.behandlingsflyt.behandling.trekkklage.TrekkKlageInformasjonskrav
import no.nav.aap.behandlingsflyt.flyt.BehandlingFlyt
import no.nav.aap.behandlingsflyt.flyt.BehandlingFlytBuilder
import no.nav.aap.behandlingsflyt.flyt.BehandlingType
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.FatteVedtakSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.KvalitetssikringsSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.MeldingOmVedtakBrevSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.klage.FastsettBehandlendeEnhetSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.klage.FormkravSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.klage.FullmektigSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.klage.KlagebehandlingKontorSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.klage.KlagebehandlingNayOppsummeringSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.klage.KlagebehandlingNaySteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.klage.OmgjøringSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.klage.OpprettholdelseSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.klage.PåklagetBehandlingSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.klage.TrekkKlageSteg
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov

object Klage : BehandlingType {
    override fun flyt(): BehandlingFlyt {

        return BehandlingFlytBuilder()
            .medSteg(
                steg = TrekkKlageSteg,
                vurderingsbehovRelevanteForSteg = listOf(Vurderingsbehov.KLAGE_TRUKKET),
                informasjonskrav = listOf(TrekkKlageInformasjonskrav)
            )
            .medSteg(steg = PåklagetBehandlingSteg)
            .medSteg(steg = FullmektigSteg)
            .medSteg(steg = FormkravSteg)
            .medSteg(steg = FastsettBehandlendeEnhetSteg)
            .medSteg(steg = KlagebehandlingKontorSteg)
            .medSteg(steg = KvalitetssikringsSteg)
            .medSteg(steg = KlagebehandlingNaySteg)
            .medSteg(steg = KlagebehandlingNayOppsummeringSteg)
            .sluttÅOppdatereFaktagrunnlag()
            .medSteg(steg = FatteVedtakSteg)
            .medSteg(steg = OmgjøringSteg)
            .medSteg(steg = OpprettholdelseSteg)
            .medSteg(steg = MeldingOmVedtakBrevSteg)
            .build()
    }
}