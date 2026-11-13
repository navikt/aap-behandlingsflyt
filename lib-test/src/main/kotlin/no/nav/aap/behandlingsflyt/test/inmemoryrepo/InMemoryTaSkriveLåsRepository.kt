package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.lås.BehandlingSkrivelås
import no.nav.aap.behandlingsflyt.sakogbehandling.lås.SakSkrivelås
import no.nav.aap.behandlingsflyt.sakogbehandling.lås.Skrivelås
import no.nav.aap.behandlingsflyt.sakogbehandling.lås.TaSkriveLåsRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import java.util.UUID

object InMemoryTaSkriveLåsRepository : TaSkriveLåsRepository {
    private val sakVersjoner = mutableMapOf<SakId, Long>()
    private val behandlingVersjoner = mutableMapOf<BehandlingId, Long>()
    private val lock = Any()

    override fun lås(sakId: SakId, behandlingId: BehandlingId): Skrivelås = synchronized(lock) {
        Skrivelås(låsSak(sakId), låsBehandling(behandlingId))
    }

    override fun lås(behandlingUUid: UUID): Skrivelås {
        val behandling = InMemoryBehandlingRepository.hent(BehandlingReferanse(behandlingUUid))
        return lås(behandling.sakId, behandling.id)
    }

    override fun låsSak(sakId: SakId): SakSkrivelås = synchronized(lock) {
        InMemorySakRepository.hent(sakId)
        SakSkrivelås(sakId, sakVersjoner.getOrPut(sakId) { 0L })
    }

    override fun låsBehandling(behandlingId: BehandlingId): BehandlingSkrivelås = synchronized(lock) {
        val behandling = InMemoryBehandlingRepository.hent(behandlingId)
        BehandlingSkrivelås(behandlingId, behandlingVersjoner.getOrPut(behandlingId) { behandling.versjon })
    }

    override fun withLås(sakId: SakId, behandlingId: BehandlingId, block: (Skrivelås) -> Unit) {
        synchronized(lock) {
            val skrivelås = lås(sakId, behandlingId)
            block(skrivelås)
            verifiserSkrivelås(skrivelås)
        }
    }

    override fun withLåstBehandling(behandlingId: BehandlingId, block: (BehandlingSkrivelås) -> Unit) {
        synchronized(lock) {
            val skrivelås = låsBehandling(behandlingId)
            block(skrivelås)
            verifiserSkrivelås(skrivelås)
        }
    }

    override fun verifiserSkrivelås(skrivelås: Skrivelås) {
        synchronized(lock) {
            require(sakVersjoner[skrivelås.sakSkrivelås.id] == skrivelås.sakSkrivelås.versjon) {
                "Sakens versjon er endret siden skrivelåsen ble tatt"
            }
            require(behandlingVersjoner[skrivelås.behandlingSkrivelås.id] == skrivelås.behandlingSkrivelås.versjon) {
                "Behandlingens versjon er endret siden skrivelåsen ble tatt"
            }
            verifiserSkrivelås(skrivelås.sakSkrivelås)
            verifiserSkrivelås(skrivelås.behandlingSkrivelås)
        }
    }

    override fun verifiserSkrivelås(skrivelås: SakSkrivelås) {
        synchronized(lock) {
            require(sakVersjoner[skrivelås.id] == skrivelås.versjon) {
                "Sakens versjon er endret siden skrivelåsen ble tatt"
            }
            sakVersjoner[skrivelås.id] = skrivelås.versjon + 1
        }
    }

    override fun verifiserSkrivelås(skrivelås: BehandlingSkrivelås) {
        synchronized(lock) {
            require(behandlingVersjoner[skrivelås.id] == skrivelås.versjon) {
                "Behandlingens versjon er endret siden skrivelåsen ble tatt"
            }
            behandlingVersjoner[skrivelås.id] = skrivelås.versjon + 1
        }
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        // Låser og versjoner tilhører den enkelte saken og behandlingen.
    }

    override fun slett(behandlingId: BehandlingId) {
        // BehandlingRepository eier behandlingens levetid.
    }
}
