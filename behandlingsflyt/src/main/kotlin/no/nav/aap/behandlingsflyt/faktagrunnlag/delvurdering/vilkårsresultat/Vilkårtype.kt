package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat

enum class Vilkårtype(
    val kode: String,
    val spesielleInnvilgelsesÅrsaker: List<Innvilgelsesårsak>,
    val avslagsårsaker: List<Avslagsårsak>,
    val hjemmel: String,
    val obligatorisk: Boolean = true
) {
    ALDERSVILKÅRET(
        kode = "AAP-4",
        spesielleInnvilgelsesÅrsaker = listOf(),
        avslagsårsaker = listOf(
            Avslagsårsak.BRUKER_OVER_67,
            Avslagsårsak.BRUKER_UNDER_18,
            Avslagsårsak.MANGLENDE_DOKUMENTASJON
        ),
        hjemmel = "§ 11-4"
    ),
    LOVVALG(
        kode = "AAP-3",
        spesielleInnvilgelsesÅrsaker = listOf(),
        avslagsårsaker = listOf(
            Avslagsårsak.MANGLENDE_DOKUMENTASJON,
            Avslagsårsak.IKKE_MEDLEM,
            Avslagsårsak.NORGE_IKKE_KOMPETENT_STAT
        ),
        hjemmel = "§ 11-3"
    ),
    SYKDOMSVILKÅRET(
        kode = "AAP-5",
        spesielleInnvilgelsesÅrsaker = listOf(Innvilgelsesårsak.YRKESSKADE_ÅRSAKSSAMMENHENG, Innvilgelsesårsak.STUDENT),
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
            Innvilgelsesårsak.VURDERES_FOR_UFØRETRYGD
        ),
        avslagsårsaker = listOf(
            Avslagsårsak.MANGLENDE_DOKUMENTASJON,
            Avslagsårsak.IKKE_BEHOV_FOR_OPPFOLGING
        ),
        hjemmel = "§ 11-6"
    ),
    MEDLEMSKAP(
        kode = "AAP-2",
        spesielleInnvilgelsesÅrsaker = listOf(),
        avslagsårsaker = listOf(
            Avslagsårsak.MANGLENDE_DOKUMENTASJON,
            Avslagsårsak.IKKE_MEDLEM,
            Avslagsårsak.IKKE_MEDLEM_FORUTGÅENDE,
        ),
        hjemmel = "§ 11-2"
    ),
    GRUNNLAGET(
        kode = "AAP-19",
        spesielleInnvilgelsesÅrsaker = listOf(),
        avslagsårsaker = listOf(
            Avslagsårsak.MANGLENDE_DOKUMENTASJON
        ),
        hjemmel = "§ 11-19"
    ),
    SYKEPENGEERSTATNING(
        kode = "AAP-13",
        spesielleInnvilgelsesÅrsaker = listOf(),
        avslagsårsaker = listOf(
            Avslagsårsak.MANGLENDE_DOKUMENTASJON
        ),
        hjemmel = "§ 11-13",
        obligatorisk = false
    ),
}