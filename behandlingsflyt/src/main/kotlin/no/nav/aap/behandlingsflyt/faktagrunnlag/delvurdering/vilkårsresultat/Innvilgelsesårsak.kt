package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat

enum class Innvilgelsesårsak(val kode: String, val hjemmel: String) {
    YRKESSKADE_ÅRSAKSSAMMENHENG("11-5_11-22", "§ 11-5 jamfør § 11-22 1. ledd"),
    SYKEPENGEERSTATNING("11-5_11-13", "§ 11-13"),
    VURDERES_FOR_UFØRETRYGD("11-5_11-18", "§ 11-18"),
    STUDENT("11-5_11-14", "§ 11-14"), // Kun for bakoverkompatibilitet
}