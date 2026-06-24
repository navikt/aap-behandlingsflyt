package no.nav.aap.behandlingsflyt.behandling.vilkår.samordning.annenfullytelse

import no.nav.aap.behandlingsflyt.behandling.samordning.SamordningGradering
import no.nav.aap.behandlingsflyt.behandling.samordning.SamordningService
import no.nav.aap.behandlingsflyt.behandling.vilkår.Vilkårsvurderer
import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.SamordningYtelseVurderingGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UføreGrunnlag
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.komponenter.verdityper.Prosent.Companion.`100_PROSENT`

data class SamordningAnnenFullYtelseFaktagrunnlag(
    val rettighetsperiode: Periode,
    val samordningTidslinje: Tidslinje<SamordningGradering>,
    val uføreTidslinje: Tidslinje<Prosent>,
    val samordningGrunnlag: SamordningYtelseVurderingGrunnlag?,
    val uføreRegisterGrunnlag: UføreGrunnlag?,
    val uføreVurderingGrunnlag: SamordningUføreGrunnlag?,
) : Faktagrunnlag

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
                        faktagrunnlag = grunnlag,
                    )
                else
                    Vilkårsvurdering(
                        utfall = Utfall.IKKE_OPPFYLT,
                        manuellVurdering = false,
                        begrunnelse = "Full ytelse ${samordninger.joinToString { (navn, _) -> navn }}",
                        avslagsårsak = Avslagsårsak.ANNEN_FULL_YTELSE,
                        faktagrunnlag = grunnlag,
                    )
            }

        vilkår.nullstillTidslinje()
        vilkår.leggTilVurderinger(vurderinger.begrensetTil(grunnlag.rettighetsperiode))
    }
}