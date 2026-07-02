package no.nav.aap.behandlingsflyt.behandling.vilkår.samordning.annenfullytelse

import no.nav.aap.behandlingsflyt.behandling.avslag11_27.Avslag11_27Grunnlag
import no.nav.aap.behandlingsflyt.behandling.samordning.SamordningGradering
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
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravGrunnlag
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Prosent.Companion.`100_PROSENT`

data class SamordningAnnenFullYtelseFaktagrunnlag(
    val rettighetsperiode: Periode,
    val samordningTidslinje: Tidslinje<SamordningGradering>,
    val samordningGrunnlag: SamordningYtelseVurderingGrunnlag?,
    val uføreRegisterGrunnlag: UføreGrunnlag?,
    val uføreVurderingGrunnlag: SamordningUføreGrunnlag?,
    val avslag1127grunnlag: Avslag11_27Grunnlag?,
    val kravGrunnlag: KravGrunnlag?,
) : Faktagrunnlag

class SamordningAnnenFullYtelseVilkår(vilkårsresultat: Vilkårsresultat) :
    Vilkårsvurderer<SamordningAnnenFullYtelseFaktagrunnlag> {

    private val vilkår: Vilkår = vilkårsresultat.leggTilHvisIkkeEksisterer(Vilkårtype.SAMORDNING)

    override fun vurder(grunnlag: SamordningAnnenFullYtelseFaktagrunnlag) {
        val uføreTidslinje = grunnlag.uføreVurderingGrunnlag?.vurdering?.tilTidslinje().orEmpty()
        val avslag11_27Tidslinje = grunnlag.avslag1127grunnlag
            ?.tilTidslinje(grunnlag.kravGrunnlag)
            .orEmpty()

        /* NB: bevisst valg å ikke gi avslag selv om summen av samordninger blir til 100%. */
        val samordningVurderinger =
            grunnlag.samordningTidslinje.outerJoinNotNull(uføreTidslinje) { andreYtelserSamordning, samordningUføreGradering ->
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

        val avslag11_27Vurderinger = avslag11_27Tidslinje.map { vurdering ->
            if (vurdering.skalAvslås1127)
                Vilkårsvurdering(
                    utfall = Utfall.IKKE_OPPFYLT,
                    manuellVurdering = true,
                    begrunnelse = "§ 11-27 avslag",
                    avslagsårsak = Avslagsårsak.ANNEN_FULL_YTELSE_AVSLAG,
                    faktagrunnlag = grunnlag,
                )
            else
                Vilkårsvurdering(
                    utfall = Utfall.OPPFYLT,
                    manuellVurdering = true,
                    begrunnelse = "§ 11-27 ikke avslag",
                    avslagsårsak = null,
                    faktagrunnlag = grunnlag,
                )
        }

        // Avslag 11-27 prioriteres: IKKE_OPPFYLT fra avslag11_27 overstyrer samordning
        val vurderinger = samordningVurderinger.outerJoinNotNull(avslag11_27Vurderinger) { samordning, avslag1127 ->
            when {
                avslag1127?.utfall == Utfall.IKKE_OPPFYLT -> avslag1127
                samordning?.utfall == Utfall.IKKE_OPPFYLT -> samordning
                avslag1127?.utfall == Utfall.OPPFYLT -> avslag1127
                else -> samordning
            }
        }

        vilkår.nullstillTidslinje()
        vilkår.leggTilVurderinger(vurderinger.begrensetTil(grunnlag.rettighetsperiode))
    }
}