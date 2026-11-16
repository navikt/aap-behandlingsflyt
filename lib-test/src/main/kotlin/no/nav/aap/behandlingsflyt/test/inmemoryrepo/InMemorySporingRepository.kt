package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.etterlevelse.sporing.Sporing
import no.nav.aap.etterlevelse.sporing.SporingRepository
import no.nav.aap.etterlevelse.sporing.Sporingsnøkkel

object InMemorySporingRepository : SporingRepository {
    private val sporinger = mutableMapOf<Pair<BehandlingId, Sporingsnøkkel>, Sporing>()
    private val lock = Any()

    /** Som i databasen: én sporing per behandling og nøkkel, siste kjøring overskriver. */
    override fun lagre(sporing: Sporing) {
        synchronized(lock) {
            sporinger[sporing.behandlingId to sporing.sporingsnøkkel] = sporing
        }
    }

    override fun hent(behandlingId: BehandlingId, sporingsnøkkel: Sporingsnøkkel): Sporing? =
        synchronized(lock) { sporinger[behandlingId to sporingsnøkkel] }

    /** Sporing kopieres ikke mellom behandlinger. */
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) = Unit

    override fun slett(behandlingId: BehandlingId) {
        synchronized(lock) { sporinger.keys.removeAll { it.first == behandlingId } }
    }
}
