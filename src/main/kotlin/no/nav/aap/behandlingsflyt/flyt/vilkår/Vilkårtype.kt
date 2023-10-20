package no.nav.aap.behandlingsflyt.flyt.vilkår

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
    SYKDOMSVILKÅRET(
        kode = "AAP-5",
        spesielleInnvilgelsesÅrsaker = listOf(Innvilgelsesårsak.YRKESSKADE_ÅRSAKSSAMMENHENG),
        avslagsårsaker = listOf(
            Avslagsårsak.MANGLENDE_DOKUMENTASJON
        ),
        hjemmel = "§ 11-5"
    ),
    BISTANDSVILKÅRET(
        kode = "AAP-6",
        spesielleInnvilgelsesÅrsaker = listOf(),
        avslagsårsaker = listOf(
            Avslagsårsak.MANGLENDE_DOKUMENTASJON
        ),
        hjemmel = "§ 11-6"
    ),
    GRUNNLAGET(
        kode = "AAP-20",
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
    )

}