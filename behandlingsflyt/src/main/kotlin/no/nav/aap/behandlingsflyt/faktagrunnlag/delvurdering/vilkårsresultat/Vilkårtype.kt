package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat

/**
 * Er `kode` fra felles kodeverk? Eller noe vi har funnet på selv? Dokumenter plz.
 */
enum class Vilkårtype(
    val kode: String,
    val spesielleInnvilgelsesÅrsaker: List<Innvilgelsesårsak>,
    val avslagsårsaker: List<Avslagsårsak>,
    val hjemmel: String,
    val obligatorisk: Boolean = true
) {
    ALDERSVILKÅRET(
        kode = "AAP-4",
        spesielleInnvilgelsesÅrsaker = emptyList(),
        avslagsårsaker = listOf(
            Avslagsårsak.BRUKER_OVER_67,
            Avslagsårsak.BRUKER_UNDER_18,
            Avslagsårsak.MANGLENDE_DOKUMENTASJON
        ),
        hjemmel = "§ 11-4"
    ),
    LOVVALG(
        kode = "AAP-3",
        spesielleInnvilgelsesÅrsaker = emptyList(),
        avslagsårsaker = listOf(
            Avslagsårsak.MANGLENDE_DOKUMENTASJON,
            Avslagsårsak.IKKE_MEDLEM,
            Avslagsårsak.NORGE_IKKE_KOMPETENT_STAT
        ),
        hjemmel = "§ 11-3"
    ),
    SYKDOMSVILKÅRET(
        kode = "AAP-5",
        spesielleInnvilgelsesÅrsaker = listOf(
            Innvilgelsesårsak.YRKESSKADE_ÅRSAKSSAMMENHENG,
            Innvilgelsesårsak.STUDENT,
            Innvilgelsesårsak.SYKEPENGEERSTATNING,
        ),
        avslagsårsaker = listOf(
            Avslagsårsak.MANGLENDE_DOKUMENTASJON,
            Avslagsårsak.IKKE_SYKDOM_SKADE_LYTE_VESENTLIGDEL,
            Avslagsårsak.IKKE_NOK_REDUSERT_ARBEIDSEVNE,
            Avslagsårsak.IKKE_SYKDOM_AV_VISS_VARIGHET
        ),
        hjemmel = "§ 11-5"
    ),
    BISTANDSVILKÅRET(
        kode = "AAP-6",
        spesielleInnvilgelsesÅrsaker = listOf(
            Innvilgelsesårsak.STUDENT,
            Innvilgelsesårsak.ARBEIDSSØKER,
            Innvilgelsesårsak.VURDERES_FOR_UFØRETRYGD,
        ),
        avslagsårsaker = listOf(
            Avslagsårsak.MANGLENDE_DOKUMENTASJON,
            Avslagsårsak.IKKE_BEHOV_FOR_OPPFOLGING
        ),
        hjemmel = "§ 11-6"
    ),
    OVERGANGARBEIDVILKÅRET(
        kode = "AAP-17",
        spesielleInnvilgelsesÅrsaker = listOf(
            Innvilgelsesårsak.ARBEIDSSØKER,
        ),
        avslagsårsaker = listOf(
            Avslagsårsak.MANGLENDE_DOKUMENTASJON,
            Avslagsårsak.IKKE_RETT_PA_AAP_I_PERIODE_SOM_ARBEIDSSOKER
        ),
        hjemmel = "§ 11-17"
    ),
    OVERGANGUFØREVILKÅRET(
        kode = "AAP-18",
        spesielleInnvilgelsesÅrsaker = listOf(
            Innvilgelsesårsak.VURDERES_FOR_UFØRETRYGD,
        ),
        avslagsårsaker = listOf(
            Avslagsårsak.MANGLENDE_DOKUMENTASJON,
            Avslagsårsak.IKKE_RETT_PA_AAP_UNDER_BEHANDLING_AV_UFORE,
            Avslagsårsak.VARIGHET_OVERSKREDET_OVERGANG_UFORE,
        ),
        hjemmel = "§ 11-18"
    ),
    MEDLEMSKAP(
        kode = "AAP-2",
        spesielleInnvilgelsesÅrsaker = emptyList(),
        avslagsårsaker = listOf(
            Avslagsårsak.MANGLENDE_DOKUMENTASJON,
            Avslagsårsak.IKKE_MEDLEM,
            Avslagsårsak.IKKE_MEDLEM_FORUTGÅENDE,
        ),
        hjemmel = "§ 11-2"
    ),
    GRUNNLAGET(
        kode = "AAP-19",
        spesielleInnvilgelsesÅrsaker = emptyList(),
        avslagsårsaker = listOf(
            Avslagsårsak.MANGLENDE_DOKUMENTASJON
        ),
        hjemmel = "§ 11-19"
    ),
    SAMORDNING(
        kode = "AAP-27",
        spesielleInnvilgelsesÅrsaker = emptyList(),
        avslagsårsaker = listOf(
            Avslagsårsak.ANNEN_FULL_YTELSE
        ),
        hjemmel = "§ 11-27",
        obligatorisk = false
    ),

    @Deprecated("""
        Denne skal fases ut. Er med i koden for ikke å krasje DB i prod. Planen er å få saken i prod til å rekjøres, og
        deretter _slette_ raden som inneholder det gamle vilkåret.""")
    SYKEPENGEERSTATNING(
        kode = "AAP-13",
        spesielleInnvilgelsesÅrsaker = emptyList(),
        avslagsårsaker = listOf(
            Avslagsårsak.MANGLENDE_DOKUMENTASJON
        ),
        hjemmel = "§ 11-13",
        obligatorisk = false
    ),
}