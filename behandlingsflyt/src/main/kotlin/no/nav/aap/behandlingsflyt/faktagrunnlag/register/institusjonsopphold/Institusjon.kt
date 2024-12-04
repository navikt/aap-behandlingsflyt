package no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold


class Institusjon(
    val type: Institusjonstype,
    val kategori: Oppholdstype,
    val orgnr: String,
    val navn: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Institusjon

        if (type != other.type) return false
        if (kategori != other.kategori) return false
        if (orgnr != other.orgnr) return false
        if (navn != other.navn) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + kategori.hashCode()
        result = 31 * result + orgnr.hashCode()
        result = 31 * result + navn.hashCode()
        return result
    }

    override fun toString(): String {
        return "Institusjon(type=$type, kategori=$kategori, orgnr='$orgnr', navn='$navn')"
    }
}

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
    V("Varetektsfange")
}