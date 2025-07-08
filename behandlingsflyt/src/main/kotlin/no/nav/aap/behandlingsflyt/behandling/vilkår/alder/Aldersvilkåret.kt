package no.nav.aap.behandlingsflyt.behandling.vilkår.alder

import no.nav.aap.behandlingsflyt.behandling.vilkår.Vilkårsvurderer
import no.nav.aap.behandlingsflyt.behandling.vilkår.VurderingsResultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.komponenter.tidslinje.tidslinjeOf
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import java.time.YearMonth

class Aldersvilkåret(vilkårsresultat: Vilkårsresultat) : Vilkårsvurderer<Aldersgrunnlag> {
    private val vilkår: Vilkår = vilkårsresultat.leggTilHvisIkkeEksisterer(Vilkårtype.ALDERSVILKÅRET)

    override fun vurder(grunnlag: Aldersgrunnlag) {
        if (grunnlag.alderPåSøknadsdato() < 18) {
            vilkår.leggTilVurdering(
                Vilkårsperiode(
                    periode = grunnlag.periode,
                    utfall = Utfall.IKKE_OPPFYLT,
                    manuellVurdering = false,
                    avslagsårsak = Avslagsårsak.BRUKER_UNDER_18,
                    begrunnelse = null,
                    faktagrunnlag = grunnlag,
                )
            )
            return
        }

        val sisteDagAldersvilkåretErOppfylt =
            YearMonth.from(grunnlag.fyller(67)).atEndOfMonth()

        val aldersvurderinger = tidslinjeOf(
            Periode(Tid.MIN, grunnlag.fyller(18).minusDays(1)) to Vilkårsvurdering(
                utfall = Utfall.IKKE_OPPFYLT,
                avslagsårsak = Avslagsårsak.BRUKER_UNDER_18,
                manuellVurdering = false,
                begrunnelse = null,
                faktagrunnlag = grunnlag,
            ),

            Periode(grunnlag.fyller(18), sisteDagAldersvilkåretErOppfylt) to Vilkårsvurdering(
                utfall = Utfall.OPPFYLT,
                manuellVurdering = false,
                begrunnelse = null,
                faktagrunnlag = grunnlag,
            ),

            Periode(sisteDagAldersvilkåretErOppfylt.plusDays(1), Tid.MAKS) to Vilkårsvurdering(
                utfall = Utfall.IKKE_OPPFYLT,
                avslagsårsak = Avslagsårsak.BRUKER_OVER_67,
                manuellVurdering = false,
                begrunnelse = null,
                faktagrunnlag = grunnlag,
            ),
        )
            .begrensetTil(grunnlag.periode)

        vilkår.leggTilVurderinger(aldersvurderinger)
    }
}
