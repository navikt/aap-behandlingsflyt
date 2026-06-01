package no.nav.aap.behandlingsflyt.sakogbehandling.sak

/**
 * Representerer databaseId for en sak - er ikke ment å dele utenfor domenet.
 */
data class SakId(val id: Long) {
    fun toLong(): Long {
        return id
    }
    companion object {
        fun fromStringOrNull(source: String?): SakId? {
            return source?.toLongOrNull()?.let { SakId(it) }
        }
    }
}