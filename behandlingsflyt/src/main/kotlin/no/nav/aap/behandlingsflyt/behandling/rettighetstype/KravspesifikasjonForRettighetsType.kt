package no.nav.aap.behandlingsflyt.behandling.rettighetstype

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Innvilgelsesårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.komponenter.tidslinje.Tidslinje

interface KravspesifikasjonForRettighetsType {
    val rettighetstype: RettighetsType

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
        fun avslagsårsaker(vilkårsvurdering: Vilkårsvurdering?): Set<Avslagsårsak>
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

        override fun avslagsårsaker(vilkårsvurdering: Vilkårsvurdering?) =
            setOfNotNull(vilkårsvurdering?.avslagsårsak)
    }

    /** Det er ikke et krav om at vilkåret er markert som oppfylt, men det sjekkes
     * at vilkåret ikke er [Utfall.IKKE_OPPFYLT].
     *
     * Hvis vi eksplisitt setter relevante vilkår som
     * [Utfall.OPPFYLT] i stede for å ikke vurdere dem,
     * så kan vi bruke [MåVæreOppfylt]  i stedet for dette kravet.
     */
    data object SkalIkkeGiAvslag : Krav {
        override fun oppfyllesAv(vilkårsvurdering: Vilkårsvurdering?) =
            vilkårsvurdering?.utfall != Utfall.IKKE_OPPFYLT

        override fun avslagsårsaker(vilkårsvurdering: Vilkårsvurdering?) =
            setOfNotNull(vilkårsvurdering?.avslagsårsak)
    }

    data object IngenKrav : Krav {
        override fun oppfyllesAv(vilkårsvurdering: Vilkårsvurdering?) = true
        override fun avslagsårsaker(vilkårsvurdering: Vilkårsvurdering?) =
            emptySet<Avslagsårsak>()
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
                /* Straffegjennomføringsvilkåret blir alltid satt, så kan egentlig være "MåVæreOppfylt",
                  * men siden ikke alle åpne behandlinger har kjørt gjennom  dette steget, så
                  * brukes det svakere kravet `SkalIkkeGiAvslag`.
                  *
                  * Betingelse for å bytte til "MåVæreOppfylt": alle åpne behandlinger har vurdering
                  * for vilkåret `STRAFFEGJENNOMFØRING` hvor det er en periode med OPPFYLT
                  * eller IKKE_OPPFYLT.
                 */
                && SkalIkkeGiAvslag.oppfyllesAv(vilkårsresultat[Vilkårtype.STRAFFEGJENNOMFØRING])
                && kravForutgåendeMedlemskap.oppfyllesAv(vilkårsresultat[Vilkårtype.MEDLEMSKAP])
                && MåVæreOppfylt().oppfyllesAv(vilkårsresultat[Vilkårtype.LOVVALG])
                && kravSykdom.oppfyllesAv(vilkårsresultat[Vilkårtype.SYKDOMSVILKÅRET])
                && kravOvergangArbeid.oppfyllesAv(vilkårsresultat[Vilkårtype.OVERGANGARBEIDVILKÅRET])
                && kravOvergangUfør.oppfyllesAv(vilkårsresultat[Vilkårtype.OVERGANGUFØREVILKÅRET])
                && kravSykepengeerstatning.oppfyllesAv(vilkårsresultat[Vilkårtype.SYKEPENGEERSTATNING])
                && forutgåendeAap.oppfyllesAv(forutgåendeRettighetstyper)
    }

    /** Her mangler  stans/opphør fra underveis. */
    fun avslagsårsaker(vilkårsresultat: Map<Vilkårtype, Vilkårsvurdering>): Set<Avslagsårsak> {
        return MåVæreOppfylt().avslagsårsaker(vilkårsresultat[Vilkårtype.ALDERSVILKÅRET]) +
                kravBistand.avslagsårsaker(vilkårsresultat[Vilkårtype.BISTANDSVILKÅRET]) +
                MåVæreOppfylt().avslagsårsaker(vilkårsresultat[Vilkårtype.GRUNNLAGET]) +
                SkalIkkeGiAvslag.avslagsårsaker(vilkårsresultat[Vilkårtype.SAMORDNING]) +
                SkalIkkeGiAvslag.avslagsårsaker(vilkårsresultat[Vilkårtype.STRAFFEGJENNOMFØRING]) +
                kravForutgåendeMedlemskap.avslagsårsaker(vilkårsresultat[Vilkårtype.MEDLEMSKAP]) +
                MåVæreOppfylt().avslagsårsaker(vilkårsresultat[Vilkårtype.LOVVALG]) +
                kravSykdom.avslagsårsaker(vilkårsresultat[Vilkårtype.SYKDOMSVILKÅRET]) +
                kravOvergangArbeid.avslagsårsaker(vilkårsresultat[Vilkårtype.OVERGANGARBEIDVILKÅRET]) +
                kravOvergangUfør.avslagsårsaker(vilkårsresultat[Vilkårtype.OVERGANGUFØREVILKÅRET])
    }
}