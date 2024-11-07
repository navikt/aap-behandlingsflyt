package no.nav.aap.behandlingsflyt.kontrakt.sak

import com.fasterxml.jackson.annotation.JsonValue
import java.util.*

public class Saksnummer(private val identifikator: String) {

    @JsonValue
    override fun toString(): String {
        return identifikator
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Saksnummer

        return identifikator == other.identifikator
    }

    override fun hashCode(): Int {
        return identifikator.hashCode()
    }

    public companion object {

        /**
         * Gjør saksnummer "human readable"
         */
        public fun valueOf(id: Long): Saksnummer {
            return Saksnummer(
                (id * 1000).toString(36)
                    .uppercase(Locale.getDefault())
                    .replace("O", "o")
                    .replace("I", "i")
            )
        }
    }
}
