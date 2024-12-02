package no.nav.aap.behandlingsflyt.sakogbehandling.behandling

/**
 * Representerer databaseId for en behandling - er ikke ment å dele utenfor domenet.
 */
data class BehandlingId(val id: Long) {
    fun toLong(): Long {
        return id
    }
}
