package no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap

import no.nav.aap.behandlingsflyt.behandling.lovvalg.ForutgåendeMedlemskapGrunnlag
import no.nav.aap.behandlingsflyt.behandling.vilkår.Vilkårsvurderer
import no.nav.aap.behandlingsflyt.behandling.vilkår.VurderingsResultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Innvilgelsesårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.komponenter.type.Periode

class ForutgåendeMedlemskapvilkåret(
    vilkårsresultat: Vilkårsresultat,
    private val rettighetsPeriode: Periode
) : Vilkårsvurderer<ForutgåendeMedlemskapGrunnlag> {
    private val vilkår = vilkårsresultat.leggTilHvisIkkeEksisterer(Vilkårtype.MEDLEMSKAP)

    override fun vurder(grunnlag: ForutgåendeMedlemskapGrunnlag) {
        val forutgåendePeriode = Periode(rettighetsPeriode.fom.minusYears(5), rettighetsPeriode.tom)
        val manuellVurdering = grunnlag.medlemskapArbeidInntektGrunnlag?.manuellVurdering

        var vurdertManuelt = false
        val vurderingsResultat = if (manuellVurdering != null) {
            vurdertManuelt = true
            if (!manuellVurdering.harForutgåendeMedlemskap
                && (manuellVurdering.medlemMedUnntakAvMaksFemAar != true && manuellVurdering.varMedlemMedNedsattArbeidsevne != true)
            ) {
                VurderingsResultat(Utfall.IKKE_OPPFYLT, Avslagsårsak.IKKE_MEDLEM_FORUTGÅENDE, null)
            } else {
                VurderingsResultat(Utfall.OPPFYLT, null, null)
            }
        } else if (grunnlag.nyeSoknadGrunnlag == null)  {
            VurderingsResultat(Utfall.IKKE_RELEVANT, null, null)
        } else {
            val kanBehandlesAutomatisk = ForutgåendeMedlemskapLovvalgVurderingService().vurderTilhørighet(grunnlag, forutgåendePeriode).kanBehandlesAutomatisk

            val utfall = if (kanBehandlesAutomatisk) Utfall.OPPFYLT else Utfall.IKKE_VURDERT
            VurderingsResultat(utfall, null, null)
        }

        leggTilVurdering(grunnlag, vurderingsResultat, vurdertManuelt)
    }

    fun vurderOverstyrt(grunnlag: ForutgåendeMedlemskapGrunnlag) {
        val manuellVurdering = grunnlag.medlemskapArbeidInntektGrunnlag?.manuellVurdering
        val vurderingsResultat = if (!manuellVurdering!!.harForutgåendeMedlemskap
            && (manuellVurdering.medlemMedUnntakAvMaksFemAar != true && manuellVurdering.varMedlemMedNedsattArbeidsevne != true)) {
                VurderingsResultat(Utfall.IKKE_OPPFYLT, Avslagsårsak.IKKE_MEDLEM_FORUTGÅENDE, null)
            } else {
                VurderingsResultat(Utfall.OPPFYLT, null, null)
            }
        leggTilVurdering(grunnlag, vurderingsResultat, true)
    }

    fun leggTilYrkesskadeVurdering() {
        val vurderingsResultat = VurderingsResultat(Utfall.OPPFYLT, null, Innvilgelsesårsak.YRKESSKADE_ÅRSAKSSAMMENHENG)
        vilkår.leggTilVurdering(
            Vilkårsperiode(
                periode = rettighetsPeriode,
                utfall = vurderingsResultat.utfall,
                avslagsårsak = null,
                begrunnelse = vurderingsResultat.innvilgelsesårsak.toString(),
                faktagrunnlag = null,
                versjon = vurderingsResultat.versjon(),
                manuellVurdering = false
            )
        )
    }

    private fun leggTilVurdering(
        grunnlag: ForutgåendeMedlemskapGrunnlag,
        vurderingsResultat: VurderingsResultat,
        vurdertManuelt: Boolean
    ) {
        vilkår.leggTilVurdering(
            Vilkårsperiode(
                periode = rettighetsPeriode,
                utfall = vurderingsResultat.utfall,
                avslagsårsak = vurderingsResultat.avslagsårsak,
                begrunnelse = grunnlag.medlemskapArbeidInntektGrunnlag?.manuellVurdering?.begrunnelse,
                faktagrunnlag = grunnlag,
                versjon = vurderingsResultat.versjon(),
                manuellVurdering = vurdertManuelt
            )
        )
    }


}