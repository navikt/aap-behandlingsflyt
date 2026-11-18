package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.lås.BehandlingSkrivelås
import no.nav.aap.behandlingsflyt.sakogbehandling.lås.SakSkrivelås
import no.nav.aap.behandlingsflyt.sakogbehandling.lås.Skrivelås
import no.nav.aap.behandlingsflyt.sakogbehandling.lås.TaSkriveLåsRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object InMemoryTaSkriveLåsRepository : TaSkriveLåsRepository {
    private val behandlingsversjoner = ConcurrentHashMap<BehandlingId, Long>()
    private val saksversjoner = ConcurrentHashMap<SakId, Long>()

    override fun lås(sakId: SakId, behandlingId: BehandlingId): Skrivelås =
        Skrivelås(låsSak(sakId), låsBehandling(behandlingId))

    override fun withLås(sakId: SakId, behandlingId: BehandlingId, block: (Skrivelås) -> Unit) {
        val skrivelås = lås(sakId, behandlingId)
        block(skrivelås)
        verifiserSkrivelås(skrivelås)
    }

    override fun låsBehandling(behandlingId: BehandlingId): BehandlingSkrivelås {
        val behandling = InMemoryBehandlingRepository.hent(behandlingId)
        return BehandlingSkrivelås(
            behandlingId,
            behandlingsversjoner.getOrPut(behandlingId) { behandling.versjon }
        )
    }

    override fun withLåstBehandling(behandlingId: BehandlingId, block: (BehandlingSkrivelås) -> Unit) {
        val skrivelås = låsBehandling(behandlingId)
        block(skrivelås)
        verifiserSkrivelås(skrivelås)
    }

    override fun lås(behandlingUUid: UUID): Skrivelås {
        val behandling = InMemoryBehandlingRepository.hent(BehandlingReferanse(behandlingUUid))
        return lås(behandling.sakId, behandling.id)
    }

    override fun låsSak(sakId: SakId): SakSkrivelås {
        InMemorySakRepository.hent(sakId)
        return SakSkrivelås(sakId, saksversjoner.getOrPut(sakId) { 0L })
    }

    override fun verifiserSkrivelås(skrivelås: Skrivelås) {
        verifiserSkrivelås(skrivelås.behandlingSkrivelås)
        verifiserSkrivelås(skrivelås.sakSkrivelås)
    }

    override fun verifiserSkrivelås(skrivelås: SakSkrivelås) {
        check(saksversjoner.replace(skrivelås.id, skrivelås.versjon, skrivelås.versjon + 1)) {
            "[Utviklerfeil] Ugyldig skrivelås for sak ${skrivelås.id}"
        }
    }

    override fun verifiserSkrivelås(skrivelås: BehandlingSkrivelås) {
        check(behandlingsversjoner.replace(skrivelås.id, skrivelås.versjon, skrivelås.versjon + 1)) {
            "[Utviklerfeil] Ugyldig skrivelås for behandling ${skrivelås.id}"
        }
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        // Skrivelåser kopieres ikke mellom behandlinger.
    }

    override fun slett(behandlingId: BehandlingId) {
        behandlingsversjoner.remove(behandlingId)
    }
}
