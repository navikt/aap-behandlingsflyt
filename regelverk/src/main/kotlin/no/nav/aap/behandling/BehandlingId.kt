package no.nav.aap.behandling

/**
 * Representerer databaseId for en behandling - er ikke ment å dele utenfor domenet.
 */
data class BehandlingId(val id: Long) {
    fun toLong(): Long {
        return id
    }
}