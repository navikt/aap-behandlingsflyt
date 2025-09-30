package no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository

interface Aktivitetsplikt11_9Repository : Repository {
    fun hentHvisEksisterer(behandlingId: BehandlingId): Aktivitetsplikt11_9Grunnlag?
    fun lagre(behandlingId: BehandlingId, vurderinger: List<Aktivitetsplikt11_9Vurdering>)
}