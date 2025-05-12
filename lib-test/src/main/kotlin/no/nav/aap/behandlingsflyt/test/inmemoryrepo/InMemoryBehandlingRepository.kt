package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.StegTilstand
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Årsak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicLong

object InMemoryBehandlingRepository : BehandlingRepository {
    private val log = LoggerFactory.getLogger(javaClass)

    private val idSeq = AtomicLong(10000)
    private val memory = HashMap<BehandlingId, Behandling>()
    private val memoryStegHistorikk = HashMap<BehandlingId, List<StegTilstand>>()
    private val lock = Object()

    override fun opprettBehandling(
        sakId: SakId,
        årsaker: List<Årsak>,
        typeBehandling: TypeBehandling,
        forrigeBehandlingId: BehandlingId?
    ): Behandling {
        synchronized(lock) {
            val id = BehandlingId(idSeq.andIncrement)
            if (memory.containsKey(id)) {
                throw IllegalArgumentException("Behandling id finnes allerede $id")
            }
            if (forrigeBehandlingId != null && !memory.containsKey(forrigeBehandlingId)) {
                throw IllegalArgumentException("Behandling id finnes allerede $id")
            }
            val behandling = Behandling(
                id = id,
                forrigeBehandlingId = forrigeBehandlingId,
                sakId = sakId,
                typeBehandling = typeBehandling,
                versjon = 1,
                årsaker = årsaker
            )
            memory[id] = behandling

            return behandling
        }
    }

    override fun finnSisteBehandlingFor(sakId: SakId, behandlingstypeFilter: List<TypeBehandling>): Behandling? {
        synchronized(lock) {
            return memory.values
                .filter { behandling -> behandling.sakId == sakId }
                .filter { behandling -> behandling.typeBehandling() in behandlingstypeFilter }
                .maxOrNull()
        }
    }

    override fun hentAlleFor(sakId: SakId, behandlingstypeFilter: List<TypeBehandling>): List<Behandling> {
        synchronized(lock) {
            return memory.values
                .filter { behandling -> behandling.sakId == sakId }
                .filter { behandling -> behandling.typeBehandling() in behandlingstypeFilter }
        }
    }

    override fun hent(behandlingId: BehandlingId): Behandling {
        synchronized(lock) {
            return memory.getValue(behandlingId)
        }
    }

    override fun hentStegHistorikk(behandlingId: BehandlingId): List<StegTilstand> {
        synchronized(lock) {
            return memoryStegHistorikk[behandlingId] ?: emptyList()
        }
    }

    override fun hent(referanse: BehandlingReferanse): Behandling {
        synchronized(lock) {
            log.info("Henter behandling med referanse $referanse.")
            return memory.values.single { behandling -> behandling.referanse == referanse }
        }
    }

    override fun hentBehandlingType(behandlingId: BehandlingId): TypeBehandling {
        synchronized(lock) {
            return memory.getValue(behandlingId).typeBehandling()
        }
    }

    override fun oppdaterÅrsaker(
        behandling: Behandling,
        årsaker: List<Årsak>
    ) {

    }

    override fun finnSøker(referanse: BehandlingReferanse): Person {
        TODO("Not yet implemented")
    }

    override fun oppdaterBehandlingStatus(
        behandlingId: BehandlingId,
        status: Status
    ) {
        synchronized(lock) {
            // ** Disclainer **
            // Benytter reflection her for å ikke kompromitere produksjonskode
            // Anser det som bedre enn å tilpasse produksjonskode til testing
            // da tilpassing av koden vil tilgjengliggjøre funksjonalitet på behandlingen
            // som ikke er tiltenkt at man skal manipulere på.
            //
            // Før du vurderer å gjøre noe tilsvarende, ta en prat med tech-lead.

            val behandling = memory.getValue(behandlingId)
            val field = behandling::class.java.getDeclaredField("status")
            field.trySetAccessible()
            field.set(behandling, status)
        }
    }

    override fun leggTilNyttAktivtSteg(
        behandlingId: BehandlingId,
        tilstand: StegTilstand
    ) {
        synchronized(lock) {
            val stegHistorikk = memoryStegHistorikk[behandlingId]?.map {
                StegTilstand(
                    tidspunkt = it.tidspunkt(),
                    stegStatus = it.status(),
                    stegType = it.steg(),
                    aktiv = false
                )
            } ?: emptyList()
            memoryStegHistorikk[behandlingId] = stegHistorikk.plus(tilstand).sorted()
        }
    }

    override fun markerSavepoint() {
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
    }
}