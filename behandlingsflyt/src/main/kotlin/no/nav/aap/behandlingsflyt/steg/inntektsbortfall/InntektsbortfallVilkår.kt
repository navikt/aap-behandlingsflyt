package no.nav.aap.behandlingsflyt.steg.inntektsbortfall

import no.nav.aap.behandlingsflyt.faktagrunnlag.vilkårsresultat.Vilkårsresultat
import no.nav.aap.inntektsbortfall.InntektsbortfallGrunnlag
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.misc.Vilkårsvurderer
import no.nav.aap.vilkårsresultat.Avslagsårsak
import no.nav.aap.vilkårsresultat.Utfall
import no.nav.aap.vilkårsresultat.Vilkår
import no.nav.aap.vilkårsresultat.Vilkårsperiode
import no.nav.aap.vilkårsresultat.Vilkårtype

class InntektsbortfallVilkår(
    vilkårsresultat: Vilkårsresultat,
    private val rettighetsPeriode: Periode
) : Vilkårsvurderer<InntektsbortfallGrunnlag> {
    private val vilkår: Vilkår = vilkårsresultat.leggTilHvisIkkeEksisterer(Vilkårtype.INNTEKTSBORTFALL)

    override fun vurder(grunnlag: InntektsbortfallGrunnlag) {
        val vurdering = if (grunnlag.inntektsbortfallKanBehandlesAutomatisk.kanBehandlesAutomatisk) {
            Vilkårsperiode(
                periode = rettighetsPeriode,
                utfall = Utfall.OPPFYLT,
                manuellVurdering = false,
                begrunnelse = "Bruker under 62 år, eller har hatt inntekt siste år over 1G, eller har hatt inntekt over 3G siste tre år.",
                faktagrunnlag = grunnlag
            )
        } else {
            val manuellVurdering = grunnlag.manuellVurdering
            if (manuellVurdering == null) {
                Vilkårsperiode(
                    periode = rettighetsPeriode,
                    utfall = Utfall.IKKE_VURDERT,
                    manuellVurdering = false,
                    begrunnelse = null,
                    faktagrunnlag = grunnlag
                )
            } else if (manuellVurdering.rettTilUttak) {
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