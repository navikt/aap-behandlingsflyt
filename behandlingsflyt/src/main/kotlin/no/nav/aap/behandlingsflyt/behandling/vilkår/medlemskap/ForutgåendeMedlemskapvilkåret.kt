package no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap

import no.nav.aap.behandlingsflyt.behandling.lovvalg.ForutgåendeMedlemskapGrunnlag
import no.nav.aap.behandlingsflyt.behandling.vilkår.Vilkårsvurderer
import no.nav.aap.behandlingsflyt.behandling.vilkår.VurderingsResultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.ManuellVurderingForForutgåendeMedlemskap
import no.nav.aap.komponenter.type.Periode

class ForutgåendeMedlemskapvilkåret(
    vilkårsresultat: Vilkårsresultat,
    private val rettighetsPeriode: Periode,
    private val manuellVurdering: ManuellVurderingForForutgåendeMedlemskap?
) : Vilkårsvurderer<ForutgåendeMedlemskapGrunnlag> {
    private val vilkår = vilkårsresultat.leggTilHvisIkkeEksisterer(Vilkårtype.MEDLEMSKAP)

    override fun vurder(grunnlag: ForutgåendeMedlemskapGrunnlag) {
        val forutgåendePeriode = Periode(rettighetsPeriode.fom.minusYears(5), rettighetsPeriode.tom)

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

        leggTilVurdering(forutgåendePeriode, grunnlag, vurderingsResultat, vurdertManuelt)
    }

    private fun leggTilVurdering(
        periode: Periode,
        grunnlag: ForutgåendeMedlemskapGrunnlag,
        vurderingsResultat: VurderingsResultat,
        vurdertManuelt: Boolean
    ) {
        vilkår.leggTilVurdering(
            Vilkårsperiode(
                periode = periode,
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