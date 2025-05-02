package no.nav.aap.behandlingsflyt.behandling.beregning

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Innvilgelsesårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.RepositoryProvider

class AvklarFaktaBeregningService(
    private val vilkårsresultatRepository: VilkårsresultatRepository,
) {
    constructor(repositoryProvider: RepositoryProvider) : this(
        vilkårsresultatRepository = repositoryProvider.provide(),
    )


    fun skalFastsetteGrunnlag(behandlingId: BehandlingId): Boolean {
        val vilkårsresultat = vilkårsresultatRepository.hent(behandlingId)

        val sykdomsvilkåret = vilkårsresultat.finnVilkår(Vilkårtype.SYKDOMSVILKÅRET)
        val bistandsvilkåret = vilkårsresultat.finnVilkår(Vilkårtype.BISTANDSVILKÅRET)
        val lovvalgvilkåret = vilkårsresultat.finnVilkår(Vilkårtype.LOVVALG)
        val bistandsvilkåretEllerSykepengerErstatningHvisIkke =
            (bistandsvilkåret.harPerioderSomErOppfylt() && sykdomsvilkåret.harPerioderSomErOppfylt()) || (bistandsvilkåret.vilkårsperioder()
                .all { it.innvilgelsesårsak == Innvilgelsesårsak.SYKEPENGEERSTATNING })

        return bistandsvilkåretEllerSykepengerErstatningHvisIkke
                && lovvalgvilkåret.harPerioderSomErOppfylt()
    }
}