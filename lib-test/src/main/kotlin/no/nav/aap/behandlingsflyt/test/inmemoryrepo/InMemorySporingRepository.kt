package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.etterlevelse.sporing.Sporing
import no.nav.aap.etterlevelse.sporing.SporingRepository
import no.nav.aap.etterlevelse.sporing.Sporingsnøkkel
import java.util.concurrent.ConcurrentHashMap

object InMemorySporingRepository : SporingRepository {
    private val sporinger = ConcurrentHashMap<Pair<BehandlingId, Sporingsnøkkel>, Sporing>()

    override fun lagre(sporing: Sporing) {
        sporinger[sporing.behandlingId to sporing.sporingsnøkkel] = sporing.copy(grunnlag = sporing.grunnlag.deepCopy())
    }

    override fun hent(behandlingId: BehandlingId, sporingsnøkkel: Sporingsnøkkel): Sporing? =
        sporinger[behandlingId to sporingsnøkkel]?.let { it.copy(grunnlag = it.grunnlag.deepCopy()) }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        // Sporing gjelder bare behandlingen der den ble opprettet.
    }

    override fun slett(behandlingId: BehandlingId) {
        sporinger.entries.removeIf { it.key.first == behandlingId }
    }
}
