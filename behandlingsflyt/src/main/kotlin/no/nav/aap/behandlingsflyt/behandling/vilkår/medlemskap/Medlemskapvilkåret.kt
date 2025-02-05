package no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap

import no.nav.aap.behandlingsflyt.behandling.lovvalg.MedlemskapLovvalgGrunnlag
import no.nav.aap.behandlingsflyt.behandling.vilkår.Vilkårsvurderer
import no.nav.aap.behandlingsflyt.behandling.vilkår.VurderingsResultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.ManuellVurderingForLovvalgMedlemskap
import no.nav.aap.komponenter.type.Periode

class Medlemskapvilkåret(
    vilkårsresultat: Vilkårsresultat,
    private val rettighetsPeriode: Periode,
    private val manuellVurderingForLovvalgMedlemskap: ManuellVurderingForLovvalgMedlemskap?
) : Vilkårsvurderer<MedlemskapLovvalgGrunnlag> {
    private val vilkår = vilkårsresultat.leggTilHvisIkkeEksisterer(Vilkårtype.MEDLEMSKAP)

    override fun vurder(grunnlag: MedlemskapLovvalgGrunnlag) {
        var vurdertManuelt = false
        val vurderingsResultat = if (manuellVurderingForLovvalgMedlemskap != null) {
            vurdertManuelt = true
            val lovvalgsLand = manuellVurderingForLovvalgMedlemskap.lovvalgVedSøknadsTidspunkt.lovvalgsEØSLand!!
            val manglerEØS = lovvalgsLand !in enumValues<EØSLand>().map { it }
            val varMedlemIFolketrygd = manuellVurderingForLovvalgMedlemskap.medlemskapVedSøknadsTidspunkt.varMedlemIFolketrygd

            if (!varMedlemIFolketrygd) {
                VurderingsResultat(Utfall.IKKE_OPPFYLT, Avslagsårsak.IKKE_MEDLEM, null)
            } else if (manglerEØS) {
                VurderingsResultat(Utfall.IKKE_OPPFYLT, Avslagsårsak.IKKE_OPPFYLT_OPPHOLDSKRAV_EØS, null)
            } else {
                VurderingsResultat(Utfall.OPPFYLT, null, null)
            }
        } else if (grunnlag.nyeSoknadGrunnlag == null)  {
            VurderingsResultat(Utfall.IKKE_RELEVANT, null, null)
        } else {
            val kanBehandlesAutomatisk = MedlemskapLovvalgVurderingService().vurderTilhørighet(grunnlag, rettighetsPeriode).kanBehandlesAutomatisk
            val utfall = if (kanBehandlesAutomatisk) Utfall.OPPFYLT else Utfall.IKKE_VURDERT
            VurderingsResultat(utfall, null, null)
        }

        leggTilVurdering(rettighetsPeriode, grunnlag, vurderingsResultat, vurdertManuelt)
    }

    private fun leggTilVurdering(
        periode: Periode,
        grunnlag: MedlemskapLovvalgGrunnlag,
        vurderingsResultat: VurderingsResultat,
        vurdertManuelt: Boolean
    ) {
        vilkår.leggTilVurdering(
            Vilkårsperiode(
                periode = periode,
                utfall = vurderingsResultat.utfall,
                avslagsårsak = vurderingsResultat.avslagsårsak,
                begrunnelse = null,
                faktagrunnlag = grunnlag,
                versjon = vurderingsResultat.versjon(),
                manuellVurdering = vurdertManuelt
            )
        )
    }
}