package no.nav.aap.behandlingsflyt.forretningsflyt.behandlingstyper

import no.nav.aap.behandlingsflyt.flyt.BehandlingFlyt
import no.nav.aap.behandlingsflyt.flyt.BehandlingFlytBuilder
import no.nav.aap.behandlingsflyt.flyt.BehandlingType
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.FatteVedtakSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.MeldingOmVedtakBrevSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.aktivitetsplikt.AvbrytAktivitetspliktbehandlingSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.aktivitetsplikt.IverksettBruddSteg
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.aktivitetsplikt.VurderAktivitetsplikt11_7Steg
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov

object Aktivitetsplikt : BehandlingType {
    override fun flyt(): BehandlingFlyt {
        return BehandlingFlytBuilder()
            .medSteg(
                steg = AvbrytAktivitetspliktbehandlingSteg, vurderingsbehovRelevanteForSteg = listOf(
                    Vurderingsbehov.AKTIVITETSPLIKTBEHANDLING_AVBRUTT
                )
            )
            .medSteg(steg = VurderAktivitetsplikt11_7Steg, vurderingsbehovRelevanteForSteg = Vurderingsbehov.alle())
            .sluttÅOppdatereFaktagrunnlag()
            .medSteg(steg = FatteVedtakSteg, vurderingsbehovRelevanteForSteg = Vurderingsbehov.alle())
            .medSteg(steg = IverksettBruddSteg, vurderingsbehovRelevanteForSteg = Vurderingsbehov.alle())
            .medSteg(steg = MeldingOmVedtakBrevSteg, vurderingsbehovRelevanteForSteg = Vurderingsbehov.alle())
            .build()
    }
}