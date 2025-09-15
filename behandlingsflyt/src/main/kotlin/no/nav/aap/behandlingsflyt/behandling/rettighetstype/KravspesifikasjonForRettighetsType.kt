package no.nav.aap.behandlingsflyt.behandling.rettighetstype

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Innvilgelsesårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype

interface KravspesifikasjonForRettighetsType {
    val kravBistand: Krav
    val kravForutgåendeMedlemskap: Krav
    val kravSykdom: Krav
    val kravOvergangUfør: Krav
    val kravOvergangArbeid: Krav

    sealed interface Krav {
        fun oppfyllesAv(vilkårsvurdering: Vilkårsvurdering?): Boolean
    }

    data class MåVæreOppfylt(val akseptableInnvilgelsesårsaker: List<Innvilgelsesårsak?>) : Krav {
        constructor(vararg innvilgelsesårsak: Innvilgelsesårsak?) : this(
            if (innvilgelsesårsak.isEmpty()) listOf(null)
            else innvilgelsesårsak.toList()
        )

        override fun oppfyllesAv(vilkårsvurdering: Vilkårsvurdering?) =
            vilkårsvurdering != null
                    && vilkårsvurdering.erOppfylt()
                    && vilkårsvurdering.innvilgelsesårsak in akseptableInnvilgelsesårsaker
    }

    data object SkalIkkeGiAvslag : Krav {
        override fun oppfyllesAv(vilkårsvurdering: Vilkårsvurdering?) =
            vilkårsvurdering?.utfall != Utfall.IKKE_OPPFYLT
    }

    data object IngenKrav : Krav {
        override fun oppfyllesAv(vilkårsvurdering: Vilkårsvurdering?) = true
    }

    fun oppfyllesAv(vilkårsresultat: Map<Vilkårtype, Vilkårsvurdering>): Boolean {
        return MåVæreOppfylt().oppfyllesAv(vilkårsresultat[Vilkårtype.ALDERSVILKÅRET])
                && kravBistand.oppfyllesAv(vilkårsresultat[Vilkårtype.BISTANDSVILKÅRET])
                && MåVæreOppfylt().oppfyllesAv(vilkårsresultat[Vilkårtype.GRUNNLAGET])
                && SkalIkkeGiAvslag.oppfyllesAv(vilkårsresultat[Vilkårtype.SAMORDNING])
                && kravForutgåendeMedlemskap.oppfyllesAv(vilkårsresultat[Vilkårtype.MEDLEMSKAP])
                && MåVæreOppfylt().oppfyllesAv(vilkårsresultat[Vilkårtype.LOVVALG])
                && kravSykdom.oppfyllesAv(vilkårsresultat[Vilkårtype.SYKDOMSVILKÅRET])
                && kravOvergangArbeid.oppfyllesAv(vilkårsresultat[Vilkårtype.OVERGANGARBEIDVILKÅRET])
                && kravOvergangUfør.oppfyllesAv(vilkårsresultat[Vilkårtype.OVERGANGUFØREVILKÅRET])
    }
}