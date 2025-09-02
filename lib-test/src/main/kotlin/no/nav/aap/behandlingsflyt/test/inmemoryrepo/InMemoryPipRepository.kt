package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.pip.IdentPåSak
import no.nav.aap.behandlingsflyt.pip.PipRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId

object InMemoryPipRepository: PipRepository {
    override fun finnIdenterPåSak(saksnummer: Saksnummer): List<IdentPåSak> {
        return emptyList()
    }

    override fun finnIdenterPåBehandling(behandlingReferanse: BehandlingReferanse): List<IdentPåSak> {
        return emptyList()
    }

    override fun kopier(
        fraBehandling: BehandlingId,
        tilBehandling: BehandlingId
    ) {
    }

    override fun slett(behandlingId: BehandlingId) {
    }
}