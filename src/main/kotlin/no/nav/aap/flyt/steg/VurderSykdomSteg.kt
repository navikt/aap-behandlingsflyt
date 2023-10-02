package no.nav.aap.flyt.steg

import no.nav.aap.behandlingsflyt.domene.behandling.BehandlingTjeneste
import no.nav.aap.behandlingsflyt.domene.behandling.Vilkårstype
import no.nav.aap.behandlingsflyt.domene.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.domene.behandling.grunnlag.sykdom.SykdomsTjeneste
import no.nav.aap.behandlingsflyt.domene.vilkår.sykdom.SykdomsFaktagrunnlag
import no.nav.aap.behandlingsflyt.domene.vilkår.sykdom.Sykdomsvilkår
import no.nav.aap.behandlingsflyt.flate.behandling.periode.PeriodeTilVurderingTjeneste
import no.nav.aap.flyt.StegType

class VurderSykdomSteg : BehandlingSteg {
    override fun utfør(input: StegInput): StegResultat {
        val behandling = BehandlingTjeneste.hent(input.kontekst.behandlingId)

        val periodeTilVurdering =
            PeriodeTilVurderingTjeneste.utled(behandling = behandling, vilkår = Vilkårstype.SYKDOMSVILKÅRET)

        if (periodeTilVurdering.isNotEmpty()) {
            val sykdomsvilkåret = behandling.vilkårsresultat().finnVilkår(Vilkårstype.SYKDOMSVILKÅRET)
            val sykdomsGrunnlag = SykdomsTjeneste.hentHvisEksisterer(behandlingId = behandling.id)

            if (sykdomsGrunnlag != null && sykdomsGrunnlag.erKonsistent()) {
                for (periode in periodeTilVurdering) {
                    val faktagrunnlag = SykdomsFaktagrunnlag(
                        periode.fom,
                        periode.tom,
                        sykdomsGrunnlag.yrkesskadevurdering,
                        sykdomsGrunnlag.sykdomsvurdering!!
                    )
                    Sykdomsvilkår(sykdomsvilkåret).vurder(faktagrunnlag)
                }
            }

            if (sykdomsvilkåret.harPerioderSomIkkeErVurdert(periodeTilVurdering) || sykdomsGrunnlag?.erKonsistent() != true) {
                return StegResultat(listOf(Definisjon.AVKLAR_SYKDOM))
            }
        }
        return StegResultat()
    }

    override fun type(): StegType {
        return StegType.AVKLAR_SYKDOM
    }
}
