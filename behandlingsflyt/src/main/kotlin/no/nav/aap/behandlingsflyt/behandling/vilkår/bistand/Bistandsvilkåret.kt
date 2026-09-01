package no.nav.aap.behandlingsflyt.behandling.vilkår.bistand

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsvurderer
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.Bistandsvurdering
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.orEmpty

object Bistandsvilkåret: Vilkårsvurderer<BistandFaktagrunnlag> {
    override val vilkårtype = Vilkårtype.BISTANDSVILKÅRET

    override fun vurder(faktagrunnlag: BistandFaktagrunnlag): Tidslinje<Vilkårsvurdering> {
        val bistandvurderingTidslinje =
            faktagrunnlag.bistandGrunnlag?.somBistandsvurderingstidslinje(faktagrunnlag.sisteDagMedMuligYtelse).orEmpty()

        return bistandvurderingTidslinje.map { bistandVurdering ->
            opprettVilkårsvurdering(
                bistandVurdering,
                faktagrunnlag
            )
        }
    }

    private fun opprettVilkårsvurdering(
        bistandsvurdering: Bistandsvurdering?,
        grunnlag: BistandFaktagrunnlag
    ): Vilkårsvurdering {
        val (utfall, avslagsårsak) = if (bistandsvurdering?.erBehovForBistand() == true) {
            Pair(Utfall.OPPFYLT, null)
        } else {
            Pair(Utfall.IKKE_OPPFYLT, Avslagsårsak.IKKE_BEHOV_FOR_OPPFOLGING)
        }

        return Vilkårsvurdering(
            utfall = utfall,
            begrunnelse = null,
            innvilgelsesårsak = null,
            avslagsårsak = avslagsårsak,
            faktagrunnlag = grunnlag,
            manuellVurdering = true,
        )
    }
}
