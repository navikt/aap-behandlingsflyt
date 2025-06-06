package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.RepositoryProvider

class VilkårService(
    private val vilkårsresultatRepository: VilkårsresultatRepository,
) {
    constructor(repositoryProvider: RepositoryProvider) : this(
        vilkårsresultatRepository = repositoryProvider.provide()
    )

    /** Fyll hull i vilkårsvurderingene for [vilkårtype] som `IKKE_VURDERT`. */
    fun ingenNyeVurderinger(
        kontekst: FlytKontekstMedPerioder,
        vilkårtype: Vilkårtype,
        begrunnelse: String? = null,
    ) {
        ingenNyeVurderinger(kontekst.behandlingId, vilkårtype, kontekst.rettighetsperiode, begrunnelse)
    }

    /** Fyll hull i vilkårsvurderingene for [vilkårtype] som `IKKE_VURDERT`. */
    fun ingenNyeVurderinger(
        behandlingId: BehandlingId,
        vilkårtype: Vilkårtype,
        periode: Periode,
        begrunnelse: String? = null,
    ) {
        val vilkårsresultat = vilkårsresultatRepository.hent(behandlingId)
        val vilkåret = vilkårsresultat.leggTilHvisIkkeEksisterer(vilkårtype)

        val ikkeVurdertePerioder = vilkåret.tidslinje().komplement(periode) {
            Vilkårsvurdering(
                utfall = Utfall.IKKE_VURDERT,
                manuellVurdering = false,
                begrunnelse = begrunnelse,
                innvilgelsesårsak = null,
                avslagsårsak = null,
                faktagrunnlag = null,
            )
        }
        vilkåret.leggTilVurderinger(ikkeVurdertePerioder)

        vilkårsresultatRepository.lagre(behandlingId, vilkårsresultat)
    }
}