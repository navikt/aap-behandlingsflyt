package no.nav.aap.behandlingsflyt.steg.samordning

import no.nav.aap.behandlingsflyt.faktagrunnlag.vilkårsresultat.Vilkårsresultat
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.misc.Vilkårsvurderer
import no.nav.aap.samordning.annenlovgivning.SamordningAnnenLovgivningFaktagrunnlag
import no.nav.aap.vilkårsresultat.Avslagsårsak
import no.nav.aap.vilkårsresultat.Utfall
import no.nav.aap.vilkårsresultat.Vilkår
import no.nav.aap.vilkårsresultat.Vilkårsvurdering
import no.nav.aap.vilkårsresultat.Vilkårtype

class SamordningAnnenLovgivningVilkår(vilkårsresultat: Vilkårsresultat) :
    Vilkårsvurderer<SamordningAnnenLovgivningFaktagrunnlag> {
    private val vilkår: Vilkår = vilkårsresultat.finnVilkår(Vilkårtype.SAMORDNING_ANNEN_LOVGIVNING)

    override fun vurder(grunnlag: SamordningAnnenLovgivningFaktagrunnlag) {

        val mottarSykestipendTidslinje: Tidslinje<Boolean> = Tidslinje(
            listOf(Segment(grunnlag.rettighetsperiode, false))
        ).leftJoin(grunnlag.sykestipendGrunnlag?.tilMottarSykestipendTidslinje().orEmpty()) { _, mottarSykestipend ->
            mottarSykestipend == true
        }.komprimer()


        val tidslinje =
            mottarSykestipendTidslinje.map { mottarSykestipend -> opprettVilkårsvurdering(mottarSykestipend, grunnlag) }

        vilkår.leggTilVurderinger(tidslinje)
    }

    private fun opprettVilkårsvurdering(
        mottarSykestipend: Boolean,
        grunnlag: SamordningAnnenLovgivningFaktagrunnlag
    ): Vilkårsvurdering {
        return if (mottarSykestipend) {
            Vilkårsvurdering(
                utfall = Utfall.IKKE_OPPFYLT,
                manuellVurdering = true,
                begrunnelse = "Mottar sykestipend",
                faktagrunnlag = grunnlag,
                avslagsårsak = Avslagsårsak.INNTEKTSTAP_DEKKES_ETTER_ANNEN_LOVGIVNING
            )
        } else {
            Vilkårsvurdering(
                utfall = Utfall.IKKE_VURDERT, // Må nulle ut perioder som ikke lenger er i listen
                manuellVurdering = true,
                begrunnelse = "Mottar ikke sykestipend",
                faktagrunnlag = grunnlag
            )
        }
    }
}