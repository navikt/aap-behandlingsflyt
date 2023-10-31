package no.nav.aap.behandlingsflyt.sak

class SakId(private val id: Long) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SakId

        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return id.toString()
    }

    fun toLong(): Long {
        return id
    }
}