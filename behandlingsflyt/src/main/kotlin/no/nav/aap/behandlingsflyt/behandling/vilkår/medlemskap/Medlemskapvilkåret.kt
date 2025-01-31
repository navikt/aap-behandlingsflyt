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

class Medlemskapvilkåret(vilkårsresultat: Vilkårsresultat, private val rettighetsPeriode: Periode): Vilkårsvurderer<MedlemskapLovvalgGrunnlag> {
    private val vilkår = vilkårsresultat.leggTilHvisIkkeEksisterer(Vilkårtype.MEDLEMSKAP)

    override fun vurder(grunnlag: MedlemskapLovvalgGrunnlag) {
        val vurderingsResultat = if (grunnlag.nyeSoknadGrunnlag == null)  {
            VurderingsResultat(Utfall.IKKE_VURDERT, null, null)
        } else {
            val kanBehandlesAutomatisk = MedlemskapLovvalgVurderingService().vurderTilhørighet(grunnlag, rettighetsPeriode).kanBehandlesAutomatisk
            val avslagsårsak = if (kanBehandlesAutomatisk) null else Avslagsårsak.MANGLENDE_DOKUMENTASJON // TODO: Noe bedre årsak?
            val utfall = if (kanBehandlesAutomatisk) Utfall.OPPFYLT else Utfall.IKKE_OPPFYLT

            VurderingsResultat(utfall, avslagsårsak, null)
        }

        leggTilVurdering(rettighetsPeriode, grunnlag, vurderingsResultat)
    }

    private fun leggTilVurdering(
        periode: Periode,
        grunnlag: MedlemskapLovvalgGrunnlag,
        vurderingsResultat: VurderingsResultat
    ) {
        vilkår.leggTilVurdering(
            Vilkårsperiode(
                periode = periode,
                utfall = vurderingsResultat.utfall,
                avslagsårsak = vurderingsResultat.avslagsårsak,
                begrunnelse = null,
                faktagrunnlag = grunnlag,
                versjon = vurderingsResultat.versjon()
            )
        )
    }
}