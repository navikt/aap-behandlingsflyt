package no.nav.aap.behandlingsflyt.steg.bistand

import no.nav.aap.behandlingsflyt.faktagrunnlag.vilkårsresultat.Vilkårsresultat
import no.nav.aap.bistand.BistandFaktagrunnlag
import no.nav.aap.bistand.Bistandsvurdering
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.misc.Vilkårsvurderer
import no.nav.aap.vilkårsresultat.Avslagsårsak
import no.nav.aap.vilkårsresultat.Utfall
import no.nav.aap.vilkårsresultat.Vilkår
import no.nav.aap.vilkårsresultat.Vilkårsvurdering
import no.nav.aap.vilkårsresultat.Vilkårtype

class Bistandsvilkåret(vilkårsresultat: Vilkårsresultat) : Vilkårsvurderer<BistandFaktagrunnlag> {
    private val vilkår: Vilkår = vilkårsresultat.finnVilkår(Vilkårtype.BISTANDSVILKÅRET)

    override fun vurder(grunnlag: BistandFaktagrunnlag) {
        val bistandvurderingTidslinje =
            grunnlag.bistandGrunnlag?.somBistandsvurderingstidslinje(grunnlag.sisteDagMedMuligYtelse).orEmpty()

        val tidslinje =
            bistandvurderingTidslinje.map { bistandVurdering -> opprettVilkårsvurdering(bistandVurdering, grunnlag) }

        vilkår.leggTilVurderinger(tidslinje)
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