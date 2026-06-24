package no.nav.aap.behandlingsflyt.behandling.vilkår.samordning.annenfullytelse

import no.nav.aap.behandlingsflyt.behandling.vilkår.Vilkårsvurderer
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.komponenter.verdityper.Prosent.Companion.`100_PROSENT`

class SamordningAnnenFullYtelseVilkår(vilkårsresultat: Vilkårsresultat) :
    Vilkårsvurderer<SamordningAnnenFullYtelseFaktagrunnlag> {

    private val vilkår: Vilkår = vilkårsresultat.leggTilHvisIkkeEksisterer(Vilkårtype.SAMORDNING)

    override fun vurder(grunnlag: SamordningAnnenFullYtelseFaktagrunnlag) {
        /* NB: bevisst valg å ikke gi avslag selv om summen av samordninger blir til 100%. */
        val vurderinger =
            grunnlag.samordningTidslinje.outerJoinNotNull(grunnlag.uføreTidslinje) { andreYtelserSamordning, samordningUføreGradering ->
                val samordningerYtelser =
                    andreYtelserSamordning?.ytelsesGraderinger.orEmpty()
                        .map { it.ytelse.toString() to it.gradering }
                val samordningUføre = listOfNotNull(samordningUføreGradering?.let { "UFØRE" to it })
                val samordninger = (samordningerYtelser + samordningUføre)
                    .filter { (_, prosent) -> prosent == `100_PROSENT` }

                if (samordninger.isEmpty())
                    Vilkårsvurdering(
                        utfall = Utfall.IKKE_VURDERT,
                        manuellVurdering = false,
                        begrunnelse = "Ikke full ytelse av samordninger",
                        avslagsårsak = null,
                        faktagrunnlag = grunnlag.samordningAvslagGrunnlag,
                    )
                else
                    Vilkårsvurdering(
                        utfall = Utfall.IKKE_OPPFYLT,
                        manuellVurdering = false,
                        begrunnelse = "Full ytelse ${samordninger.joinToString { (navn, _) -> navn }}",
                        avslagsårsak = Avslagsårsak.ANNEN_FULL_YTELSE,
                        faktagrunnlag = grunnlag.samordningAvslagGrunnlag,
                    )
            }

        vilkår.nullstillTidslinje()
        vilkår.leggTilVurderinger(vurderinger.begrensetTil(grunnlag.rettighetsperiode))
    }
}