package no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade

import java.time.LocalDate

class Yrkesskade(val ref: String, val skadedato: LocalDate) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Yrkesskade

        if (ref != other.ref) return false
        if (skadedato != other.skadedato) return false

        return true
    }

    override fun hashCode(): Int {
        var result = ref.hashCode()
        result = 31 * result + skadedato.hashCode()
        return result
    }
}
