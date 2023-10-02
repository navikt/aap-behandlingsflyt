package no.nav.aap.flyt.steg

import no.nav.aap.behandlingsflyt.domene.Periode
import no.nav.aap.behandlingsflyt.domene.behandling.BehandlingTjeneste
import no.nav.aap.behandlingsflyt.domene.behandling.Vilkår
import no.nav.aap.behandlingsflyt.domene.behandling.Vilkårstype
import no.nav.aap.behandlingsflyt.domene.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.domene.behandling.grunnlag.sykdom.SykdomsGrunnlag
import no.nav.aap.behandlingsflyt.domene.behandling.grunnlag.sykdom.SykdomsTjeneste
import no.nav.aap.behandlingsflyt.domene.behandling.grunnlag.yrkesskade.YrkesskadeGrunnlag
import no.nav.aap.behandlingsflyt.domene.behandling.grunnlag.yrkesskade.YrkesskadeTjeneste
import no.nav.aap.behandlingsflyt.flate.behandling.periode.PeriodeTilVurderingTjeneste
import no.nav.aap.flyt.StegType

class VurderYrkesskadeÅrsakssammenhengSteg : BehandlingSteg {
    override fun utfør(input: StegInput): StegResultat {
        val behandling = BehandlingTjeneste.hent(input.kontekst.behandlingId)

        val periodeTilVurdering =
            PeriodeTilVurderingTjeneste.utled(behandling = behandling, vilkår = Vilkårstype.SYKDOMSVILKÅRET)

        if (periodeTilVurdering.isNotEmpty()) {
            val yrkesskadeGrunnlag = YrkesskadeTjeneste.hentHvisEksisterer(behandlingId = behandling.id)
            val sykdomsvilkåret = behandling.vilkårsresultat().finnVilkår(Vilkårstype.SYKDOMSVILKÅRET)
            val sykdomsGrunnlag = SykdomsTjeneste.hentHvisEksisterer(behandlingId = behandling.id)

            if (erBehovForAvklaring(yrkesskadeGrunnlag, sykdomsvilkåret, periodeTilVurdering, sykdomsGrunnlag)) {
                return StegResultat(listOf(Definisjon.AVKLAR_YRKESSKADE))
            }
        }
        return StegResultat()
    }

    private fun erBehovForAvklaring(
        yrkesskadeGrunnlag: YrkesskadeGrunnlag?,
        sykdomsvilkåret: Vilkår,
        periodeTilVurdering: Set<no.nav.aap.behandlingsflyt.domene.Periode>,
        sykdomsGrunnlag: SykdomsGrunnlag?
    ): Boolean {
        return yrkesskadeGrunnlag?.yrkesskader?.yrkesskader?.isNotEmpty() == true && (sykdomsvilkåret.harPerioderSomIkkeErVurdert(
            periodeTilVurdering
        ) || sykdomsGrunnlag?.erKonsistent() != true)
    }

    override fun type(): StegType {
        return StegType.AVKLAR_YRKESSKADE
    }
}
