package no.nav.aap.behandlingsflyt.forretningsflyt.behandlingstyper

import no.nav.aap.behandlingsflyt.flyt.BehandlingFlyt
import no.nav.aap.behandlingsflyt.flyt.BehandlingFlytBuilder
import no.nav.aap.behandlingsflyt.flyt.BehandlingType
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.FatteVedtakSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.KvalitetssikringsSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.MeldingOmVedtakBrevSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.SendForvaltningsmeldingSteg
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
            )
            .medSteg(
                steg = SendForvaltningsmeldingSteg,
                vurderingsbehovRelevanteForSteg = listOf(Vurderingsbehov.MOTATT_KLAGE)
                // TODO: informasjonskrav - behov ? snakker vi her om postadresse e.l. til brevbestilling?
            )
            .medSteg(steg = PåklagetBehandlingSteg, vurderingsbehovRelevanteForSteg = Vurderingsbehov.alle())
            .medSteg(steg = FullmektigSteg, vurderingsbehovRelevanteForSteg = Vurderingsbehov.alle())
            .medSteg(steg = FormkravSteg, vurderingsbehovRelevanteForSteg = Vurderingsbehov.alle())
            .medSteg(steg = FastsettBehandlendeEnhetSteg, vurderingsbehovRelevanteForSteg = Vurderingsbehov.alle())
            .medSteg(steg = KlagebehandlingKontorSteg, vurderingsbehovRelevanteForSteg = Vurderingsbehov.alle())
            .medSteg(steg = KvalitetssikringsSteg, vurderingsbehovRelevanteForSteg = Vurderingsbehov.alle())
            .medSteg(steg = KlagebehandlingNaySteg, vurderingsbehovRelevanteForSteg = Vurderingsbehov.alle())
            .medSteg(steg = KlagebehandlingNayOppsummeringSteg, vurderingsbehovRelevanteForSteg = Vurderingsbehov.alle())
            .sluttÅOppdatereFaktagrunnlag()
            .medSteg(steg = FatteVedtakSteg, vurderingsbehovRelevanteForSteg = Vurderingsbehov.alle())
            .medSteg(steg = OmgjøringSteg, vurderingsbehovRelevanteForSteg = Vurderingsbehov.alle())
            .medSteg(steg = MeldingOmVedtakBrevSteg, vurderingsbehovRelevanteForSteg = Vurderingsbehov.alle())
            .medSteg(steg = OpprettholdelseSteg, vurderingsbehovRelevanteForSteg = Vurderingsbehov.alle())
            .build()
    }
}