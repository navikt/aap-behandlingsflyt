package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat

enum class Innvilgelsesårsak(val kode: String, val hjemmel: String) {
    YRKESSKADE_ÅRSAKSSAMMENHENG("11-5_11-22", "§ 11-5 jamfør § 11-22 1. ledd"),
    STUDENT("11-5_11-14", "§ 11-14"),
    VURDERES_FOR_UFØRETRYGD("11-5_11-18", "§ 11-18"),
}