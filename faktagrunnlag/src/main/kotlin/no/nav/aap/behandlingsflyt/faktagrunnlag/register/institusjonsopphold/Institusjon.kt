package no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold


class Institusjon(
    val type: Institusjonstype,
    val kategori: Oppholdstype,
    val orgnr:String,
    val navn: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Institusjon

        if (type != other.type) return false
        if (kategori != other.kategori) return false
        if (orgnr != other.orgnr) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + kategori.hashCode()
        result = 31 * result + orgnr.hashCode()
        return result
    }
}

enum class Institusjonstype(val beskrivelse: String) {
    AS("Alders- og sykehjem"),
    FO("Fengsel"),
    HS("Helseinstitusjon")
}

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