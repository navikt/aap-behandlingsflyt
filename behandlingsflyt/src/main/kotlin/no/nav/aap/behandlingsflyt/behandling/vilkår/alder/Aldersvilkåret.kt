package no.nav.aap.behandlingsflyt.behandling.vilkår.alder

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsvurderer
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.tidslinjeOf
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import java.time.YearMonth

class Aldersvilkåret : Vilkårsvurderer<Aldersgrunnlag> {
    override val vilkårtype: Vilkårtype = Vilkårtype.ALDERSVILKÅRET

    override fun vurder(faktagrunnlag: Aldersgrunnlag): Tidslinje<Vilkårsvurdering> {
        if (faktagrunnlag.fødselsdato.alderPåDato(
                faktagrunnlag.vurderingsdato.plusMonths(faktagrunnlag.grenseForAntallMånederFørFylte18)
            ) < 18
        ) {
            return tidslinjeOf(
                faktagrunnlag.periode to Vilkårsvurdering(
                    utfall = Utfall.IKKE_OPPFYLT,
                    manuellVurdering = false,
                    avslagsårsak = Avslagsårsak.BRUKER_UNDER_18,
                    begrunnelse = null,
                    faktagrunnlag = faktagrunnlag
                )
            )
        }

        val sisteDagAldersvilkåretErOppfylt =
            YearMonth.from(faktagrunnlag.fyller(67)).atEndOfMonth()

        return tidslinjeOf(
            Periode(Tid.MIN, faktagrunnlag.fyller(18).minusDays(1)) to Vilkårsvurdering(
                utfall = Utfall.IKKE_OPPFYLT,
                avslagsårsak = Avslagsårsak.BRUKER_UNDER_18,
                manuellVurdering = false,
                begrunnelse = null,
                faktagrunnlag = faktagrunnlag,
            ),

            Periode(faktagrunnlag.fyller(18), sisteDagAldersvilkåretErOppfylt) to Vilkårsvurdering(
                utfall = Utfall.OPPFYLT,
                manuellVurdering = false,
                begrunnelse = null,
                faktagrunnlag = faktagrunnlag,
            ),

            Periode(sisteDagAldersvilkåretErOppfylt.plusDays(1), Tid.MAKS) to Vilkårsvurdering(
                utfall = Utfall.IKKE_OPPFYLT,
                avslagsårsak = Avslagsårsak.BRUKER_OVER_67,
                manuellVurdering = false,
                begrunnelse = null,
                faktagrunnlag = faktagrunnlag,
            ),
        )
            .begrensetTil(faktagrunnlag.periode)
    }
}
