package no.nav.aap.behandlingsflyt.behandling.vilkår.bistand

import no.nav.aap.behandlingsflyt.behandling.vilkår.Vilkårsvurderer
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.Bistandsvurdering
import no.nav.aap.komponenter.tidslinje.orEmpty

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
