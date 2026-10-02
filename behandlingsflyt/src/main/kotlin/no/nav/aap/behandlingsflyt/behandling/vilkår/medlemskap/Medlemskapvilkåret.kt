package no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap

import no.nav.aap.behandlingsflyt.behandling.lovvalg.MedlemskapLovvalgFaktaGrunnlag
import no.nav.aap.behandlingsflyt.behandling.vilkår.Vilkårsvurderer
import no.nav.aap.behandlingsflyt.behandling.vilkår.VurderingsResultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.gjeldendeVurderinger
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.komponenter.type.Periode

class Medlemskapvilkåret(
    vilkårsresultat: Vilkårsresultat,
    private val rettighetsPeriode: Periode,
    private val vurderingstype: VurderingType? = null,
) : Vilkårsvurderer<MedlemskapLovvalgFaktaGrunnlag> {
    private val vilkår = vilkårsresultat.leggTilHvisIkkeEksisterer(Vilkårtype.LOVVALG)

    override fun vurder(grunnlag: MedlemskapLovvalgFaktaGrunnlag) {
        val manuelleVurderinger = grunnlag.medlemskapArbeidInntektGrunnlag?.vurderinger.orEmpty()
            .filter { !it.erAutomatiskVurdert() }
        val brukManuellVurderingForLovvalgMedlemskap = manuelleVurderinger.isNotEmpty()

        if (brukManuellVurderingForLovvalgMedlemskap) {
            val gjeldendeVurderinger = manuelleVurderinger.gjeldendeVurderinger()

            val vilkårsvurderinger = gjeldendeVurderinger
                .map { vurdering ->
                    if (vurdering.lovvalgslandErAnnetLandIEØSEllerLandMedAvtale()) {
                        Vilkårsvurdering(
                            utfall = Utfall.IKKE_OPPFYLT,
                            avslagsårsak = Avslagsårsak.NORGE_IKKE_KOMPETENT_STAT,
                            begrunnelse = null,
                            faktagrunnlag = grunnlag,
                            manuellVurdering = true
                        )
                    } else if (!vurdering.medlemIFolketrygd()) {
                        Vilkårsvurdering(
                            utfall = Utfall.IKKE_OPPFYLT,
                            avslagsårsak = Avslagsårsak.IKKE_MEDLEM,
                            begrunnelse = null,
                            faktagrunnlag = grunnlag,
                            manuellVurdering = true
                        )
                    } else {
                        Vilkårsvurdering(
                            utfall = Utfall.OPPFYLT,
                            begrunnelse = null,
                            faktagrunnlag = grunnlag,
                            manuellVurdering = true
                        )
                    }
                }
                .komprimer()
                .begrensetTil(rettighetsPeriode)

            vilkår.leggTilVurderinger(vilkårsvurderinger)

        } else if (grunnlag.nyeSoknadGrunnlag == null) {
            val vurderingsResultat = VurderingsResultat(Utfall.IKKE_RELEVANT, null, null)
            leggTilVurdering(rettighetsPeriode, grunnlag, vurderingsResultat, false)
        } else {
            // Lagrede automatiske vurderinger har foreløpig ingen effekt på behandlingen
            // Utleder på nytt
            val kanBehandlesAutomatisk = MedlemskapLovvalgVurderingService()
                .vurderTilhørighet(grunnlag, rettighetsPeriode)
                .medMetrikker(grunnlag, vurderingstype)
                .kanBehandlesAutomatisk
            val utfall = if (kanBehandlesAutomatisk) Utfall.OPPFYLT else Utfall.IKKE_VURDERT
            val vurderingsResultat = VurderingsResultat(utfall, null, null)
            leggTilVurdering(rettighetsPeriode, grunnlag, vurderingsResultat, false)
        }
    }

    private fun leggTilVurdering(
        periode: Periode,
        grunnlag: MedlemskapLovvalgFaktaGrunnlag,
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