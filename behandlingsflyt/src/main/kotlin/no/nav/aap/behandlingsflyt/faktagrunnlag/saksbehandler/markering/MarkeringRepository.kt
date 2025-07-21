package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.markering

import no.nav.aap.behandlingsflyt.kontrakt.behandling.MarkeringType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository

interface MarkeringRepository: Repository {
    fun lagre(markering: Markering)
    fun hentAktiveMarkeringerForBehandling(behandling: BehandlingId): List<Markering>
    fun deaktiverMarkering(behandling: BehandlingId, type: MarkeringType)
}