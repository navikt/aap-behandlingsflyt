package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat

/**
 * Er `kode` fra felles kodeverk? Eller noe vi har funnet på selv? Dokumenter plz.
 */
enum class Avslagsårsak(val kode: String, val hjemmel: String) {
    BRUKER_UNDER_18(kode = "11-4-1-1", hjemmel = "§ 11-4 1. ledd"),
    BRUKER_OVER_67(kode = "11-4-1-2", hjemmel = "§ 11-4 1. ledd"),
    MANGLENDE_DOKUMENTASJON(kode = "21-3", hjemmel = "§ 21-3, § 11-1"),// FIXME: Dette er neppe rett med 11-1,
    IKKE_SYKDOM_AV_VISS_VARIGHET(kode = "11-5-1", "§ 11-5"),
    IKKE_SYKDOM_SKADE_LYTE_VESENTLIGDEL(kode = "11-5-1", "§ 11-5"),
    IKKE_NOK_REDUSERT_ARBEIDSEVNE(kode = "11-5-1", "§ 11-5"),
    IKKE_BEHOV_FOR_OPPFOLGING(kode = "11-6-1", "§ 11-6"),
    IKKE_MEDLEM_FORUTGÅENDE(kode= "11-2", hjemmel = "§ 11-2"),
    IKKE_MEDLEM(kode= "2-1", hjemmel = "§ 2-1"),
    IKKE_OPPFYLT_OPPHOLDSKRAV_EØS(kode = "11-3-1", hjemmel = "§ 11-3 1.ledd"),
    NORGE_IKKE_KOMPETENT_STAT(kode= "EØS-forordning 883. Art 11-3-E", hjemmel = "EØS-forordning 883. Art 11-3-E"),
    ANNEN_FULL_YTELSE(kode = "11-27", hjemmel = "§ 11-27"),
    IKKE_RETT_PA_AAP_UNDER_BEHANDLING_AV_UFORE(kode = "11-18", hjemmel = "§ 11-18"),
    VARIGHET_OVERSKREDET_OVERGANG_UFORE(kode = "11-18", hjemmel = "§ 11-18 1. ledd"),
    VARIGHET_OVERSKREDET_ARBEIDSSØKER(kode = "11-17", hjemmel = "§ 11-17"),
    IKKE_RETT_PA_AAP_I_PERIODE_SOM_ARBEIDSSOKER(kode = "11-17", hjemmel = "§ 11-17")
}
