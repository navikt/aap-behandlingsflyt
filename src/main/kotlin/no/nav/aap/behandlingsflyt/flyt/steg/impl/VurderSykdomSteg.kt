package no.nav.aap.behandlingsflyt.flyt.steg.impl

import no.nav.aap.behandlingsflyt.domene.behandling.BehandlingTjeneste
import no.nav.aap.behandlingsflyt.domene.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegInput
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.flyt.steg.StegType
import no.nav.aap.behandlingsflyt.flyt.vilkår.Vilkårstype
import no.nav.aap.behandlingsflyt.flyt.vilkår.sykdom.SykdomsFaktagrunnlag
import no.nav.aap.behandlingsflyt.flyt.vilkår.sykdom.Sykdomsvilkår
import no.nav.aap.behandlingsflyt.grunnlag.sykdom.SykdomsTjeneste

class VurderSykdomSteg : BehandlingSteg {
    override fun utfør(input: StegInput): StegResultat {
        val behandling = BehandlingTjeneste.hent(input.kontekst.behandlingId)

        val periodeTilVurdering =
            PeriodeTilVurderingTjeneste.utled(behandling = behandling, vilkår = Vilkårstype.SYKDOMSVILKÅRET)

        if (periodeTilVurdering.isNotEmpty()) {
            val sykdomsGrunnlag = SykdomsTjeneste.hentHvisEksisterer(behandlingId = behandling.id)

            if (sykdomsGrunnlag != null && sykdomsGrunnlag.erKonsistent()) {
                for (periode in periodeTilVurdering) {
                    val faktagrunnlag = SykdomsFaktagrunnlag(
                        periode.fom,
                        periode.tom,
                        sykdomsGrunnlag.yrkesskadevurdering,
                        sykdomsGrunnlag.sykdomsvurdering!!
                    )
                    Sykdomsvilkår(behandling.vilkårsresultat()).vurder(faktagrunnlag)
                }
            }
            val sykdomsvilkåret = behandling.vilkårsresultat().finnVilkår(Vilkårstype.SYKDOMSVILKÅRET)

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
