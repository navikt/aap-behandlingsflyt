package no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade

import java.time.LocalDate

data class Yrkesskade(
    val ref: String,
    val saksnummer: Int?,
    val kildesystem: String,
    val skadedato: LocalDate?,
    val vedtaksdato: LocalDate? = null,
    val skadeart: String? = null,
    val diagnose: String? = null,
    val skadekombinasjoner: List<SkadekombinasjonRegister>? = null,
    val skadekombinasjonerTekst: String? = null,
) {
    companion object {
        fun skadekombinasjonerTilSkadebeskrivelse(list: List<SkadekombinasjonRegister>?): String =
            list?.joinToString(", ") {
                "${
                    it.skadetype.replaceFirstChar { c -> c.uppercase() }.lowercase()
                        .replaceFirstChar { c -> c.uppercase() }
                } i ${it.kroppsdel.lowercase()}"
            } ?: ""
    }
}

data class SkadekombinasjonRegister(
    val kroppsdel: String,
    val skadetype: String,
) {
    companion object {
        fun fromString(str: String): SkadekombinasjonRegister? {
            val regex = Regex("""kroppsdel=([^,]+), skadetype=([^)]+)""")
            val match = regex.find(str)
            return match?.let {
                SkadekombinasjonRegister(
                    kroppsdel = it.groupValues[1].trim(),
                    skadetype = it.groupValues[2].trim()
                )
            }
        }
    }
}
