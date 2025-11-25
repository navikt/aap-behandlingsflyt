package no.nav.aap.behandlingsflyt.behandling.rettighetstype

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Innvilgelsesårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.komponenter.tidslinje.Tidslinje

interface KravspesifikasjonForRettighetsType {
    val kravBistand: Krav
    val kravForutgåendeMedlemskap: Krav
    val kravSykdom: Krav
    val kravOvergangUfør: Krav
    val kravOvergangArbeid: Krav
    val kravSykepengeerstatning: Krav

    val forutgåendeAap: ForutgåendeKrav

    /** Krav som gjelder for perioden som vurderes. */
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

    interface ForutgåendeKrav {
        fun oppfyllesAv(forutgåendeRettighetstyper: Tidslinje<RettighetsType>): Boolean
    }

    data object IngenKravOmForutgåendeAAP : ForutgåendeKrav {
        override fun oppfyllesAv(forutgåendeRettighetstyper: Tidslinje<RettighetsType>) = true
    }

    /** For at dette kravet skal være oppfylt for en gitt periode, så må medlemmet i
     * perioden fra forutgående opphør (eller rettighetsperiode.fom hvis ingen forutgående opphør)
     * og før perioden vi vurderer, hatt minst en periode med rett til AAP etter en av de oppgitte
     * rettighetstypene.
     *
     * NB. Vi har ikke støtte for opphør, så situasjonen for opphør kan ikke skje enda.
     */
    data class KravOmForutgåendeAAP(val rettighetsTyper: Set<RettighetsType>) : ForutgåendeKrav {
        constructor(vararg rettighetsTyper: RettighetsType) : this(rettighetsTyper.toSet())

        init {
            check(rettighetsTyper.isNotEmpty()) { "Krav kan aldri være oppfylt." }
        }

        override fun oppfyllesAv(forutgåendeRettighetstyper: Tidslinje<RettighetsType>) =
            forutgåendeRettighetstyper.segmenter().any { it.verdi in rettighetsTyper }
    }

    fun oppfyllesAv(forutgåendeRettighetstyper: Tidslinje<RettighetsType>, vilkårsresultat: Map<Vilkårtype, Vilkårsvurdering>): Boolean {
        return MåVæreOppfylt().oppfyllesAv(vilkårsresultat[Vilkårtype.ALDERSVILKÅRET])
                && kravBistand.oppfyllesAv(vilkårsresultat[Vilkårtype.BISTANDSVILKÅRET])
                && MåVæreOppfylt().oppfyllesAv(vilkårsresultat[Vilkårtype.GRUNNLAGET])
                && SkalIkkeGiAvslag.oppfyllesAv(vilkårsresultat[Vilkårtype.SAMORDNING])
                && kravForutgåendeMedlemskap.oppfyllesAv(vilkårsresultat[Vilkårtype.MEDLEMSKAP])
                && MåVæreOppfylt().oppfyllesAv(vilkårsresultat[Vilkårtype.LOVVALG])
                && kravSykdom.oppfyllesAv(vilkårsresultat[Vilkårtype.SYKDOMSVILKÅRET])
                && kravOvergangArbeid.oppfyllesAv(vilkårsresultat[Vilkårtype.OVERGANGARBEIDVILKÅRET])
                && kravOvergangUfør.oppfyllesAv(vilkårsresultat[Vilkårtype.OVERGANGUFØREVILKÅRET])
                && kravSykepengeerstatning.oppfyllesAv(vilkårsresultat[Vilkårtype.SYKEPENGEERSTATNING])
                && forutgåendeAap.oppfyllesAv(forutgåendeRettighetstyper)
    }
}