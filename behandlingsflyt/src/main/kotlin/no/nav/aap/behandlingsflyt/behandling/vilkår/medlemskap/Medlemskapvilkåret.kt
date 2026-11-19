package no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap

import no.nav.aap.behandlingsflyt.behandling.lovvalg.MedlemskapLovvalgFaktagrunnlag
import no.nav.aap.behandlingsflyt.behandling.vilkår.Vilkårsvurderer
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.ManuellVurderingForLovvalgMedlemskap
import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.tidslinjeOf
import no.nav.aap.komponenter.type.Periode

class Medlemskapvilkåret(
    vilkårsresultat: Vilkårsresultat,
    private val rettighetsPeriode: Periode,
) : Vilkårsvurderer<MedlemskapLovvalgFaktagrunnlag> {
    private val vilkår = vilkårsresultat.leggTilHvisIkkeEksisterer(Vilkårtype.LOVVALG)

    override fun vurder(grunnlag: MedlemskapLovvalgFaktagrunnlag) {
        val automatiskTidslinje = automatiskTidslinje(grunnlag)
        val manuellTidslinje = grunnlag.gjeldendeManuelleVurderinger().map { vurdering -> manuellVurdering(vurdering, grunnlag) }

        // Manuelle vurderinger overstyrer de automatiske der de finnes
        val vilkårsvurderinger = automatiskTidslinje
            .mergePrioriterHøyre(manuellTidslinje)
            .komprimer()
            .begrensetTil(rettighetsPeriode)

        vilkår.leggTilVurderinger(vilkårsvurderinger)
    }

    private fun automatiskTidslinje(grunnlag: MedlemskapLovvalgFaktagrunnlag): Tidslinje<Vilkårsvurdering> {
        if (grunnlag.register.nyeSoknadGrunnlag == null) {
            return Tidslinje(rettighetsPeriode, automatiskVurdering(Utfall.IKKE_RELEVANT, grunnlag))
        }

        // Gjenbruker samme instans per utfall slik at sammenhengende perioder med likt utfall slås sammen
        val oppfylt = automatiskVurdering(Utfall.OPPFYLT, grunnlag)
        val ikkeVurdert = automatiskVurdering(Utfall.IKKE_VURDERT, grunnlag)

        return tidslinjeOf(rettighetsPeriode to ikkeVurdert).mergePrioriterHøyre(
            grunnlag.delvurderinger.kanVurderesAutomatiskTidslinje()
                .mapValue { kanBehandlesAutomatisk -> if (kanBehandlesAutomatisk) oppfylt else ikkeVurdert }
        )
    }

    private fun automatiskVurdering(utfall: Utfall, grunnlag: MedlemskapLovvalgFaktagrunnlag) = Vilkårsvurdering(
        utfall = utfall,
        begrunnelse = null,
        faktagrunnlag = grunnlag,
        manuellVurdering = false,
    )

    private fun manuellVurdering(
        vurdering: ManuellVurderingForLovvalgMedlemskap,
        grunnlag: MedlemskapLovvalgFaktagrunnlag
    ): Vilkårsvurdering = when {
        vurdering.lovvalgslandErAnnetLandIEØSEllerLandMedAvtale() -> Vilkårsvurdering(
            utfall = Utfall.IKKE_OPPFYLT,
            avslagsårsak = Avslagsårsak.NORGE_IKKE_KOMPETENT_STAT,
            begrunnelse = null,
            faktagrunnlag = grunnlag,
            manuellVurdering = true
        )

        !vurdering.medlemIFolketrygd() -> Vilkårsvurdering(
            utfall = Utfall.IKKE_OPPFYLT,
            avslagsårsak = Avslagsårsak.IKKE_MEDLEM,
            begrunnelse = null,
            faktagrunnlag = grunnlag,
            manuellVurdering = true
        )

        else -> Vilkårsvurdering(
            utfall = Utfall.OPPFYLT,
            begrunnelse = null,
            faktagrunnlag = grunnlag,
            manuellVurdering = true
        )
    }
}
