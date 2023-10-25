package no.nav.aap.behandlingsflyt.sak

class Saksnummer(private val identifikator: String) {

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
}
