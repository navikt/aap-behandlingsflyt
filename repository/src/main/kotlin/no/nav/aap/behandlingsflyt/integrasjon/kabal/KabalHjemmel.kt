package no.nav.aap.behandlingsflyt.integrasjon.kabal

import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.Hjemmel

//https://github.com/navikt/klage-kodeverk/blob/main/src/main/kotlin/no/nav/klage/kodeverk/hjemmel/Hjemmel.kt
enum class KabalHjemmel {
    FTRL_11_2,
    FTRL_11_3,
    FTRL_11_4,
    FTRL_11_5,
    FTRL_11_6,
    FTRL_11_7_11_8_11_9,
    FTRL_11_10,
    FTRL_11_12,
    FTRL_11_14,
    FTRL_11_15,
    FTRL_11_13,
    FTRL_11_17,
    FTRL_11_18,
    FTRL_11_19_11_20,
    FTRL_11_22,
    FTRL_11_23,
    FTRL_11_24,
    FTRL_11_25,
    FTRL_11_26,
    FTRL_11_27_11_28,
    FTRL_11_29,
    FTRL_11_31,
    FTRL_21_3;
}

fun Hjemmel.tilKabalHjemmel(): KabalHjemmel {
    return when (this) {
        Hjemmel.FOLKETRYGDLOVEN_11_2 -> KabalHjemmel.FTRL_11_2
        Hjemmel.FOLKETRYGDLOVEN_11_5 -> KabalHjemmel.FTRL_11_5
        Hjemmel.FOLKETRYGDLOVEN_11_6 -> KabalHjemmel.FTRL_11_6
        Hjemmel.FOLKETRYGDLOVEN_11_10_FRITAK -> KabalHjemmel.FTRL_11_10
        Hjemmel.FOLKETRYGDLOVEN_11_13 -> KabalHjemmel.FTRL_11_13
        Hjemmel.FOLKETRYGDLOVEN_11_17 -> KabalHjemmel.FTRL_11_17
        Hjemmel.FOLKETRYGDLOVEN_11_18 -> KabalHjemmel.FTRL_11_18

        Hjemmel.FOLKETRYGDLOVEN_11_19,
        Hjemmel.FOLKETRYGDLOVEN_11_20 -> KabalHjemmel.FTRL_11_19_11_20

        Hjemmel.FOLKETRYGDLOVEN_11_22 -> KabalHjemmel.FTRL_11_22
        Hjemmel.FOLKETRYGDLOVEN_11_23_UUTNYTTET_ARB_EVNE -> KabalHjemmel.FTRL_11_23
        Hjemmel.FOLKETRYGDLOVEN_11_24 -> KabalHjemmel.FTRL_11_24
        Hjemmel.FOLKETRYGDLOVEN_11_25 -> KabalHjemmel.FTRL_11_25
        Hjemmel.FOLKETRYGDLOVEN_11_26 -> KabalHjemmel.FTRL_11_26

        Hjemmel.FOLKETRYGDLOVEN_11_27,
        Hjemmel.FOLKETRYGDLOVEN_11_28 -> KabalHjemmel.FTRL_11_27_11_28
    }
}