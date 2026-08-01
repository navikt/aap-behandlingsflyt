package no.nav.aap.medlemskap

enum class Kilde {
    SØKNAD, PDL, MEDL, AA_REGISTERET, A_INNTEKT, EREG
}

enum class Indikasjon {
    I_NORGE, UTENFOR_NORGE
}

enum class VurdertPeriode(val beskrivelse: String) {
    INNEVÆRENDE_OG_FORRIGE_MND("Inneværende og to forrige måneder"),
    SØKNADSTIDSPUNKT("Søknadstidspunkt"),
    SISTE_5_ÅR("Siste 5 år")
}