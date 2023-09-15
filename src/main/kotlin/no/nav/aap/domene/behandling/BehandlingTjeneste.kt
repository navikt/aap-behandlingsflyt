package no.nav.aap.domene.behandling

import no.nav.aap.domene.behandling.grunnlag.GrunnlagKopierer
import java.util.*
import java.util.concurrent.atomic.AtomicLong

object BehandlingTjeneste {
    private var behandliger = HashMap<Long, Behandling>()
    private val key = AtomicLong()

    private val LOCK = Object()

    fun hent(behandlingId: Long): Behandling {
        synchronized(LOCK) {
            return behandliger.getValue(behandlingId)
        }
    }

    fun finnSisteBehandlingFor(sakId: Long): Behandling? {
        synchronized(LOCK) {
            return behandliger.values
                .filter { behandling -> behandling.sakId == sakId }
                .maxOrNull()
        }
    }

    fun opprettBehandling(sakId: Long, årsaker: List<Årsak>): Behandling {
        synchronized(LOCK) {
            val sisteBehandlingFor = finnSisteBehandlingFor(sakId)
            val erSisteBehandlingAvsluttet = sisteBehandlingFor?.status()?.erAvsluttet() ?: true

            if (!erSisteBehandlingAvsluttet) {
                throw IllegalStateException("Siste behandling er ikke avsluttet")
            }

            val key1 = key.addAndGet(1)
            val behandlingType = utledBehandlingType(sisteBehandlingFor != null)
            val behandling = Behandling(id = key1, sakId = sakId, type = behandlingType, årsaker = årsaker)
            behandliger[key1] = behandling

            if (sisteBehandlingFor != null) {
                GrunnlagKopierer.overfør(sisteBehandlingFor, behandling)
            }

            return behandliger.getValue(key1)
        }
    }

    private fun utledBehandlingType(present: Boolean): BehandlingType {
        if (present) {
            return Revurdering
        }
        return Førstegangsbehandling
    }

    fun hentAlleFor(sakId: Long): List<Behandling> {
        synchronized(LOCK) {
            return behandliger.values
                .filter { behandling -> behandling.sakId == sakId }
        }
    }

    fun hent(referanse: UUID): Behandling {
        synchronized(LOCK) {
            return behandliger.values
                .filter { behandling -> behandling.referanse == referanse }
                .first()
        }
    }
}
