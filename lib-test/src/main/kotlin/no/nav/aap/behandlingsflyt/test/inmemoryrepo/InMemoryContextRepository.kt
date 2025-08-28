package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.log.ContextRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId

object InMemoryContextRepository : ContextRepository {

    override fun hentDataFor(behandlingReferanse: BehandlingReferanse): Map<String, String>? {
        return emptyMap()
    }

    override fun hentDataFor(saksnummer: Saksnummer): Map<String, String>? {
        return emptyMap()
    }

    override fun kopier(
        fraBehandling: BehandlingId,
        tilBehandling: BehandlingId
    ) {

    }

    override fun slett(behandlingId: BehandlingId) {

    }
}