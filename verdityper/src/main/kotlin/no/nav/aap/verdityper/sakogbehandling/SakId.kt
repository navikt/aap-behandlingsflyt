package no.nav.aap.verdityper.sakogbehandling

/**
 * Representerer databaseId for en sak - er ikke ment å dele utenfor domenet.
 */
data class SakId(private val id: Long) {
    fun toLong(): Long {
        return id
    }
}