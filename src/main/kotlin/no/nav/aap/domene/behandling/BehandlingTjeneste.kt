package no.nav.aap.domene.behandling

import java.util.Optional
import java.util.concurrent.atomic.AtomicLong

object BehandlingTjeneste {
    private var behandliger = HashMap<Long, Behandling>()
    private val key = AtomicLong()

    private val LOCK = Object()

    fun hentBehandling(behandlingId: Long): Behandling {
        synchronized(LOCK) {
            return behandliger.getValue(behandlingId)
        }
    }

    fun finnSisteBehandlingFor(fagsakId: Long): Optional<Behandling> {
        synchronized(LOCK) {
            return Optional.ofNullable(behandliger.values
                .filter { behandling -> behandling.fagsakId == fagsakId }
                .maxOrNull())
        }
    }

    fun opprettBehandling(fagsakId: Long): Behandling {
        synchronized(LOCK) {
            val sisteBehandlingFor = finnSisteBehandlingFor(fagsakId)
            val erSisteBehandlingAvsluttet = sisteBehandlingFor.map { it.status().erAvsluttet() }.orElse(true)

            if (erSisteBehandlingAvsluttet) {
                val key1 = key.addAndGet(1)
                val behandlingType = utledBehandlingType(sisteBehandlingFor.isPresent)
                behandliger.put(key1, Behandling(key1, fagsakId, behandlingType))
                return behandliger.getValue(key1)
            } else {
                throw IllegalStateException("Siste behandling er ikke avsluttet")
            }
        }
    }

    private fun utledBehandlingType(present: Boolean): BehandlingType {
        if (present) {
            return Revurdering
        }
        return Førstegangsbehandling
    }
}
