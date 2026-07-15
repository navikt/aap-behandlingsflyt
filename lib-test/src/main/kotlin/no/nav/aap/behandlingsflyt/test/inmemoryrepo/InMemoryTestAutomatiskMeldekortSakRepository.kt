package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.test.TestAutomatiskMeldekortSakRepository

object InMemoryTestAutomatiskMeldekortSakRepository : TestAutomatiskMeldekortSakRepository {
    private val saker = mutableListOf<SakId>()

    override fun leggTil(sakId: SakId) { saker.add(sakId) }
    override fun eksisterer(sakId: SakId) = sakId in saker
    override fun hentAlle() = saker.toList()
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) = Unit
    override fun slett(behandlingId: BehandlingId) = Unit
}
