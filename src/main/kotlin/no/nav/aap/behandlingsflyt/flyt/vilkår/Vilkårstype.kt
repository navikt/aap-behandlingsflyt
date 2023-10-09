package no.nav.aap.behandlingsflyt.flyt.vilkår

enum class Vilkårstype(
    val kode: String,
    val avslagsårsaker: List<Avslagsårsak>,
    val hjemmel: String
) {
    ALDERSVILKÅRET(
        kode = "AAP-4",
        avslagsårsaker = listOf(
            Avslagsårsak.BRUKER_OVER_67,
            Avslagsårsak.BRUKER_UNDER_18,
            Avslagsårsak.MANGLENDE_DOKUMENTASJON
        ),
        hjemmel = "§ 11-4"
    ),
    SYKDOMSVILKÅRET(
        kode = "AAP-5",
        avslagsårsaker = listOf(
            Avslagsårsak.MANGLENDE_DOKUMENTASJON
        ),
        hjemmel = "§ 11-5"
    ),
    BISTANDSVILKÅRET(
        kode = "AAP-6",
        avslagsårsaker = listOf(
            Avslagsårsak.MANGLENDE_DOKUMENTASJON
        ),
        hjemmel = "§ 11-6"
    ),
    GRUNNLAGET(
        kode = "AAP-20",
        avslagsårsaker = listOf(
            Avslagsårsak.MANGLENDE_DOKUMENTASJON
        ),
        hjemmel = "§ 11-19"
    )

}