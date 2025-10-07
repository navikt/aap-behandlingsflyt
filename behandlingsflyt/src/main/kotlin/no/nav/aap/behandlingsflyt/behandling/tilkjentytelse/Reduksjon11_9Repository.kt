package no.nav.aap.behandlingsflyt.behandling.tilkjentytelse

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository

interface Reduksjon11_9Repository : Repository {
    fun hent(behandlingId: BehandlingId): List<Reduksjon11_9>
    fun lagre(behandlingId: BehandlingId, reduksjoner: List<Reduksjon11_9>)
}