package no.nav.aap.behandlingsflyt.sakogbehandling.behandling

import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import no.nav.aap.verdityper.sakogbehandling.SakId

interface BehandlingRepository {

    fun opprettBehandling(sakId: SakId, årsaker: List<Årsak>, typeBehandling: TypeBehandling): Behandling

    fun finnSisteBehandlingFor(sakId: SakId): Behandling?

    fun hentAlleFor(sakId: SakId): List<Behandling>

    fun hent(behandlingId: BehandlingId): Behandling

    fun hent(referanse: BehandlingReferanse): Behandling

    fun oppdaterÅrsaker(behandling: Behandling, årsaker: List<Årsak>)

    fun finnSøker(referanse: BehandlingReferanse): Person
}

