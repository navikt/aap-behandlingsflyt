package no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold


data class Institusjon(
    val type: Institusjonstype,
    val kategori: Oppholdstype,
    val orgnr: String,
    val navn: String
)

/**
 * Felt-koder er dokumentert her: https://confluence.adeo.no/display/NRF/INST2+-+datafelter
 */
enum class Institusjonstype(val beskrivelse: String) {
    AS("Alders- og sykehjem"),
    FO("Fengsel"),
    HS("Helseinstitusjon")
}

/**
 * Felt-koder er dokumentert her: https://confluence.adeo.no/display/NRF/INST2+-+datafelter
 */
enum class Oppholdstype(val beskrivelse: String) {
    A("Alders- og sykehjem"),
    D("Dagpasient"),
    F("Ferieopphold"),
    H("Heldøgnpasient"),
    P("Fødsel"),
    R("Opptreningsinstitusjon"),
    S("Soningsfange"),
    V("Varetektsfange"),
    UKJENT("Ukjent"),
}