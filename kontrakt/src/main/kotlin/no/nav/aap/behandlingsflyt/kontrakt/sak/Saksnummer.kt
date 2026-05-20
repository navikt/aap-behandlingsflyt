package no.nav.aap.behandlingsflyt.kontrakt.sak

import com.fasterxml.jackson.annotation.JsonCreator
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
         * Gjør saksnummer "human readable".
         */
        public fun valueOf(id: Long): Saksnummer {
            val cluster = System.getenv("NAIS_CLUSTER_NAME").orEmpty().lowercase()
            /* Prefix for saker fra testmiljøet og lokalt, slik at hvis man gjør en endring
             * på en sak i feil database (miljø), så vil ikke endringen ha noen effekt.
             */
            val prefix = when {
                cluster.contains("dev") -> "TEST_"
                cluster.contains("local") -> "LOCAL_"
                else -> ""
            }
            return Saksnummer(
                (prefix + (id * 1000).toString(36)).normalize()
            )
        }

        @JsonCreator
        @JvmStatic
        public fun fra(saksnummer: String): Saksnummer =
            Saksnummer(saksnummer.trim().normalize())

        private fun String.normalize(): String =
            this.uppercase(Locale.getDefault())
                .replace("O", "o")
                .replace("I", "i")
    }
}
