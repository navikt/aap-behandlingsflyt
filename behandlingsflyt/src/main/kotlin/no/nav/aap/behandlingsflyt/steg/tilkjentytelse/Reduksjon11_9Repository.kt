package no.nav.aap.behandlingsflyt.steg.tilkjentytelse

import no.nav.aap.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository
import no.nav.aap.tilkjentytelse.Reduksjon11_9

interface Reduksjon11_9Repository : Repository {
    fun hent(behandlingId: BehandlingId): List<Reduksjon11_9>
    fun lagre(behandlingId: BehandlingId, reduksjoner: List<Reduksjon11_9>)
}