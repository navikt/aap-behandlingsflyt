package no.nav.aap.domene.behandling

object BehandlingRepository {
    private var behandliger = HashMap<Long, Behandling>()

    private val LOCK = Object()

    fun hentBehandling(behandlingId: Long): Behandling {
        synchronized(LOCK) {
            val behandling = behandliger.getOrDefault(
                behandlingId, Behandling(id = behandlingId, type = BehandlingType.FØRSTEGANGSBEHANDLING)
            )
            behandliger[behandlingId] = behandling
            return behandling
        }
    }
}
