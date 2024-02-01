package no.nav.aap.behandlingsflyt.faktagrunnlag.vilkårsresultat

enum class Avslagsårsak(val kode: String, val hjemmel: String) {
    BRUKER_UNDER_18(kode = "11-4-1-1", hjemmel = "§ 11-4 1. ledd"),
    BRUKER_OVER_67(kode = "11-4-1-2", hjemmel = "§ 11-4 1. ledd"),
    MANGLENDE_DOKUMENTASJON(kode = "21-3", hjemmel = "§ 21-3, § 11-1"),// FIXME: Dette er neppe rett med 11-1
    IKKE_SYKDOM_SKADE_LYTE_VESENTLIGDEL(kode = "11-5-1", "§ 11-5"),
    IKKE_NOK_REDUSERT_ARBEIDSEVNE(kode = "11-5-1", "§ 11-5")
}