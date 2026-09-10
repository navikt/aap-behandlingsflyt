package no.nav.aap.behandlingsflyt.behandling.vilkår.inntektsbortfall

import no.nav.aap.behandlingsflyt.behandling.inntektsbortfall.InntektsbortfallGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsvurderer
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.somTidslinje
import no.nav.aap.komponenter.tidslinje.tidslinjeOf

object InntektsbortfallVilkår : Vilkårsvurderer<InntektsbortfallGrunnlag> {
    override val vilkårtype: Vilkårtype = Vilkårtype.INNTEKTSBORTFALL

    override fun vurder(faktagrunnlag: InntektsbortfallGrunnlag): Tidslinje<Vilkårsvurdering> {
        val rettighetsPeriode = faktagrunnlag.rettighetsPeriode
        if (faktagrunnlag.inntektsbortfallKanBehandlesAutomatisk == null) {
            return listOf(
                Vilkårsperiode(
                    periode = rettighetsPeriode,
                    utfall = Utfall.IKKE_VURDERT,
                    manuellVurdering = false,
                    begrunnelse = null,
                    innvilgelsesårsak = null
                )
            ).somTidslinje({ it.periode }, { Vilkårsvurdering(it) })
        }

        val vurdering = if (faktagrunnlag.inntektsbortfallKanBehandlesAutomatisk.kanBehandlesAutomatisk) {
            Vilkårsperiode(
                periode = rettighetsPeriode,
                utfall = Utfall.OPPFYLT,
                manuellVurdering = false,
                begrunnelse = "Bruker under 62 år, eller har hatt inntekt siste år over 1G, eller har hatt inntekt over 3G siste tre år.",
                faktagrunnlag = faktagrunnlag
            )
        } else if (faktagrunnlag.manuellVurdering == null) {
            Vilkårsperiode(
                periode = rettighetsPeriode,
                utfall = Utfall.IKKE_VURDERT,
                manuellVurdering = false,
                begrunnelse = null,
                faktagrunnlag = faktagrunnlag
            )
        } else if (faktagrunnlag.manuellVurdering.rettTilUttak) {
            Vilkårsperiode(
                periode = rettighetsPeriode,
                utfall = Utfall.IKKE_OPPFYLT,
                manuellVurdering = true,
                begrunnelse = "Bruker har rett på fullt uttak av alderspensjon.",
                faktagrunnlag = faktagrunnlag,
                avslagsårsak = Avslagsårsak.HAR_RETT_TIL_FULLT_UTTAK_ALDERSPENSJON
            )
        } else {
            Vilkårsperiode(
                periode = rettighetsPeriode,
                utfall = Utfall.OPPFYLT,
                manuellVurdering = true,
                begrunnelse = "Bruker har ikke rett på fullt uttak av alderspensjon.",
                faktagrunnlag = faktagrunnlag
            )
        }

        return tidslinjeOf(vurdering.periode to Vilkårsvurdering(vurdering))
    }
}