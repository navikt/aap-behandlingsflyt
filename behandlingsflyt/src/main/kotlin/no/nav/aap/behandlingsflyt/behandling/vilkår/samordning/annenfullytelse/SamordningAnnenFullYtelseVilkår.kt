package no.nav.aap.behandlingsflyt.behandling.vilkår.samordning.annenfullytelse

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsvurderer
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.komponenter.verdityper.Prosent.Companion.`100_PROSENT`

object SamordningAnnenFullYtelseVilkår : Vilkårsvurderer<SamordningAnnenFullYtelseFaktagrunnlag> {

    override val vilkårtype: Vilkårtype = Vilkårtype.SAMORDNING

    override fun vurder(faktagrunnlag: SamordningAnnenFullYtelseFaktagrunnlag): Tidslinje<Vilkårsvurdering> {
        val uføreTidslinje = faktagrunnlag.uføreVurderingGrunnlag?.vurdering?.tilTidslinje().orEmpty()
        val avslag11_27Tidslinje = faktagrunnlag.avslag1127grunnlag
            ?.tilTidslinje(faktagrunnlag.kravGrunnlag)
            .orEmpty()

        val samordningTidslinje = faktagrunnlag.samordningGrunnlag?.vurder().orEmpty()

        /* NB: bevisst valg å ikke gi avslag selv om summen av samordninger blir til 100%. */
        val samordningVurderinger =
            samordningTidslinje.outerJoinNotNull(uføreTidslinje) { andreYtelserSamordning, samordningUføreGradering ->
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
                        faktagrunnlag = faktagrunnlag,
                    )
                else
                    Vilkårsvurdering(
                        utfall = Utfall.IKKE_OPPFYLT,
                        manuellVurdering = false,
                        begrunnelse = "Full ytelse ${samordninger.joinToString { (navn, _) -> navn }}",
                        avslagsårsak = Avslagsårsak.ANNEN_FULL_YTELSE,
                        faktagrunnlag = faktagrunnlag,
                    )
            }

        val avslag11_27Vurderinger = avslag11_27Tidslinje.map { vurdering ->
            if (vurdering.skalAvslås1127)
                Vilkårsvurdering(
                    utfall = Utfall.IKKE_OPPFYLT,
                    manuellVurdering = true,
                    begrunnelse = "§ 11-27 avslag",
                    avslagsårsak = Avslagsårsak.ANNEN_FULL_YTELSE_AVSLAG,
                    faktagrunnlag = faktagrunnlag,
                )
            else
                Vilkårsvurdering(
                    utfall = Utfall.OPPFYLT,
                    manuellVurdering = true,
                    begrunnelse = "§ 11-27 ikke avslag",
                    avslagsårsak = null,
                    faktagrunnlag = faktagrunnlag,
                )
        }

        // Avslag 11-27 prioriteres: IKKE_OPPFYLT fra avslag11_27 overstyrer samordning
        val vurderinger = samordningVurderinger.outerJoinNotNull(avslag11_27Vurderinger) { samordning, avslag1127 ->
            when {
                avslag1127?.utfall == Utfall.IKKE_OPPFYLT -> avslag1127
                samordning?.utfall == Utfall.IKKE_OPPFYLT -> samordning
                else -> samordning
            }
        }
        return vurderinger.begrensetTil(faktagrunnlag.rettighetsperiode)
    }
}