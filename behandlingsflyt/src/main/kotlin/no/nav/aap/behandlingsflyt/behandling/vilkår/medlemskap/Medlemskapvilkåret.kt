package no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap

import no.nav.aap.behandlingsflyt.behandling.lovvalg.MedlemskapLovvalgGrunnlag
import no.nav.aap.behandlingsflyt.behandling.vilkår.Vilkårsvurderer
import no.nav.aap.behandlingsflyt.behandling.vilkår.VurderingsResultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.komponenter.type.Periode

class Medlemskapvilkåret(
    vilkårsresultat: Vilkårsresultat,
    private val rettighetsPeriode: Periode,
    val brukPeriodisertManuellVurdering: Boolean = false
) : Vilkårsvurderer<MedlemskapLovvalgGrunnlag> {
    private val vilkår = vilkårsresultat.leggTilHvisIkkeEksisterer(Vilkårtype.LOVVALG)

    override fun vurder(grunnlag: MedlemskapLovvalgGrunnlag) {
        if (brukPeriodisertManuellVurdering) {
            vurderNy(grunnlag)
        } else {
            vurderGammel(grunnlag)
        }
    }

    private fun vurderNy(grunnlag: MedlemskapLovvalgGrunnlag) {
        val brukManuellVurderingForLovvalgMedlemskap = grunnlag.medlemskapArbeidInntektGrunnlag?.vurderinger?.isNotEmpty() ?: false

        if (brukManuellVurderingForLovvalgMedlemskap) {
            // Ved manuell vurdering så må hele perioden være vurdert manuelt
            grunnlag.medlemskapArbeidInntektGrunnlag.vurderinger.map { vurdering ->
                val lovvalgsLand = vurdering.lovvalgVedSøknadsTidspunkt.lovvalgsEØSLand
                val varMedlemIFolketrygd = vurdering.medlemskapVedSøknadsTidspunkt?.varMedlemIFolketrygd

                val annetLandMedAvtaleIEØS = lovvalgsLand != null && lovvalgsLand != EØSLand.NOR && lovvalgsLand in enumValues<EØSLand>().map { it }

                val vurderingsResultat = if (annetLandMedAvtaleIEØS) {
                    VurderingsResultat(Utfall.IKKE_OPPFYLT, Avslagsårsak.NORGE_IKKE_KOMPETENT_STAT, null)
                }
                else if (varMedlemIFolketrygd != true) {
                    VurderingsResultat(Utfall.IKKE_OPPFYLT, Avslagsårsak.IKKE_MEDLEM, null)
                } else {
                    VurderingsResultat(Utfall.OPPFYLT, null, null)
                }
                // TODO fom skal ikke være null når vi har migrert
                leggTilVurdering(Periode(vurdering.fom!!, vurdering.tom ?: rettighetsPeriode.tom), grunnlag, vurderingsResultat, true)
            }
        } else if (grunnlag.nyeSoknadGrunnlag == null)  {
            val vurderingsResultat = VurderingsResultat(Utfall.IKKE_RELEVANT, null, null)
            leggTilVurdering(rettighetsPeriode, grunnlag, vurderingsResultat, false)
        } else {
            val kanBehandlesAutomatisk = MedlemskapLovvalgVurderingService().vurderTilhørighet(grunnlag, rettighetsPeriode).kanBehandlesAutomatisk
            val utfall = if (kanBehandlesAutomatisk) Utfall.OPPFYLT else Utfall.IKKE_VURDERT
            val vurderingsResultat = VurderingsResultat(utfall, null, null)
            leggTilVurdering(rettighetsPeriode, grunnlag, vurderingsResultat, false)
        }
    }

    private fun vurderGammel(grunnlag: MedlemskapLovvalgGrunnlag) {
        var vurdertManuelt = false
        val manuellVurderingForLovvalgMedlemskap = grunnlag.medlemskapArbeidInntektGrunnlag?.manuellVurdering

        val vurderingsResultat = if (manuellVurderingForLovvalgMedlemskap != null) {
            vurdertManuelt = true
            val lovvalgsLand = manuellVurderingForLovvalgMedlemskap.lovvalgVedSøknadsTidspunkt.lovvalgsEØSLand
            val varMedlemIFolketrygd = manuellVurderingForLovvalgMedlemskap.medlemskapVedSøknadsTidspunkt?.varMedlemIFolketrygd

            val annetLandMedAvtaleIEØS = lovvalgsLand != null && lovvalgsLand != EØSLand.NOR && lovvalgsLand in enumValues<EØSLand>().map { it }

            if (annetLandMedAvtaleIEØS) {
                VurderingsResultat(Utfall.IKKE_OPPFYLT, Avslagsårsak.NORGE_IKKE_KOMPETENT_STAT, null)
            }
            else if (varMedlemIFolketrygd != true) {
                VurderingsResultat(Utfall.IKKE_OPPFYLT, Avslagsårsak.IKKE_MEDLEM, null)
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