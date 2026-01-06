package no.nav.aap.behandlingsflyt.behandling.vilkår.inntektsbortfall

import no.nav.aap.behandlingsflyt.behandling.inntektsbortfall.InntektsbortfallGrunnlag
import no.nav.aap.behandlingsflyt.behandling.vilkår.Vilkårsvurderer
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.komponenter.type.Periode

class InntektsbortfallVilkår(
    vilkårsresultat: Vilkårsresultat,
    private val rettighetsPeriode: Periode
) : Vilkårsvurderer<InntektsbortfallGrunnlag> {
    private val vilkår: Vilkår = vilkårsresultat.leggTilHvisIkkeEksisterer(Vilkårtype.INNTEKTSBORTFALL)

    override fun vurder(grunnlag: InntektsbortfallGrunnlag) {
        val vurdering = if (grunnlag.inntektsbortfallGrunnlag.kanBehandlesAutomatisk) {
            Vilkårsperiode(
                periode = rettighetsPeriode,
                utfall = Utfall.OPPFYLT,
                manuellVurdering = false,
                begrunnelse = "Bruker under 62 år.",
                faktagrunnlag = grunnlag
            )
        } else if (grunnlag.manuellVurdering == null) {
            Vilkårsperiode(
                periode = rettighetsPeriode,
                utfall = Utfall.IKKE_VURDERT,
                manuellVurdering = false,
                begrunnelse = null,
                faktagrunnlag = grunnlag
            )
        } else if (grunnlag.manuellVurdering.rettTilUttak) {
            Vilkårsperiode(
                periode = rettighetsPeriode,
                utfall = Utfall.IKKE_OPPFYLT,
                manuellVurdering = true,
                begrunnelse = "Bruker har rett på fullt uttak av alderspensjon.",
                faktagrunnlag = grunnlag,
                avslagsårsak = Avslagsårsak.HAR_RETT_TIL_FULLT_UTTAK_ALDERSPENSJON
            )
        } else {
            Vilkårsperiode(
                periode = rettighetsPeriode,
                utfall = Utfall.OPPFYLT,
                manuellVurdering = true,
                begrunnelse = "Bruker har ikke rett på fullt uttak av alderspensjon.",
                faktagrunnlag = grunnlag
            )
        }

        vilkår.leggTilVurdering(vurdering)
    }

    fun settTilIkkeVurdert() {
        vilkår.leggTilVurdering(
            Vilkårsperiode(
                periode = rettighetsPeriode,
                utfall = Utfall.IKKE_VURDERT,
                manuellVurdering = false,
                begrunnelse = null,
                innvilgelsesårsak = null
            )
        )
    }
}