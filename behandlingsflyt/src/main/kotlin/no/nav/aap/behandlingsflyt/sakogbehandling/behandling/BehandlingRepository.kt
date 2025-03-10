package no.nav.aap.behandlingsflyt.sakogbehandling.behandling

import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.lookup.repository.Repository

interface BehandlingRepository : Repository {

    fun opprettBehandling(
        sakId: SakId,
        årsaker: List<Årsak>,
        typeBehandling: TypeBehandling,
        forrigeBehandlingId: BehandlingId?
    ): Behandling

    fun finnSisteBehandlingFor(sakId: SakId): Behandling?

    fun hentStegHistorikk(behandlingId: BehandlingId): List<StegTilstand>

    fun hentAlleFor(sakId: SakId): List<Behandling>

    fun hent(behandlingId: BehandlingId): Behandling

    fun hent(referanse: BehandlingReferanse): Behandling

    fun hentBehandlingType(behandlingId: BehandlingId): TypeBehandling

    fun oppdaterÅrsaker(behandling: Behandling, årsaker: List<Årsak>)

    fun finnSøker(referanse: BehandlingReferanse): Person
}

