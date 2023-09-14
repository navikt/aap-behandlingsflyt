package no.nav.aap.flyt

class Tilstand(private val type: StegType, private val status: StegStatus) {

    fun status(): StegStatus {
        return status
    }

    fun steg(): StegType {
        return type
    }

    override fun toString(): String {
        return "Tilstand(type=$type, status=$status)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Tilstand

        if (type != other.type) return false
        return status == other.status
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + status.hashCode()
        return result
    }
}
