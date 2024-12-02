package no.nav.aap.behandlingsflyt.behandling.beregning

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId

class AvklarFaktaBeregningService(private val vilkårsresultatRepository: VilkårsresultatRepository) {

    fun skalFastsetteGrunnlag(behandlingId: BehandlingId): Boolean {
        val vilkårsresultat = vilkårsresultatRepository.hent(behandlingId)

        val sykdomsvilkåret = vilkårsresultat.finnVilkår(Vilkårtype.SYKDOMSVILKÅRET)
        val bistandsvilkåret = vilkårsresultat.finnVilkår(Vilkårtype.BISTANDSVILKÅRET)
        val bistandsvilkåretEllerSykepengerErstatningHvisIkke = if (!bistandsvilkåret.harPerioderSomErOppfylt()) {
            vilkårsresultat.optionalVilkår(Vilkårtype.SYKEPENGEERSTATNING)?.harPerioderSomErOppfylt() == true
        } else {
            bistandsvilkåret.harPerioderSomErOppfylt()
        }

        return sykdomsvilkåret.harPerioderSomErOppfylt() && bistandsvilkåretEllerSykepengerErstatningHvisIkke
    }
}