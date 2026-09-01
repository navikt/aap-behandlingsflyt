package no.nav.aap.behandlingsflyt.behandling.vilkår.samordning.annenlovgivning

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsvurderer
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.orEmpty

object SamordningAnnenLovgivningVilkår :
    Vilkårsvurderer<SamordningAnnenLovgivningFaktagrunnlag> {

    override val vilkårtype: Vilkårtype = Vilkårtype.SAMORDNING_ANNEN_LOVGIVNING

    override fun vurder(faktagrunnlag: SamordningAnnenLovgivningFaktagrunnlag): Tidslinje<Vilkårsvurdering> {

        val mottarSykestipendTidslinje: Tidslinje<Boolean> = Tidslinje(
            listOf(Segment(faktagrunnlag.rettighetsperiode, false))
        ).leftJoin(faktagrunnlag.sykestipendGrunnlag?.tilMottarSykestipendTidslinje().orEmpty()) { _, mottarSykestipend ->
            mottarSykestipend == true
        }.komprimer()

        val tidslinje =
            mottarSykestipendTidslinje.map { mottarSykestipend -> opprettVilkårsvurdering(mottarSykestipend, faktagrunnlag) }

        return tidslinje
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