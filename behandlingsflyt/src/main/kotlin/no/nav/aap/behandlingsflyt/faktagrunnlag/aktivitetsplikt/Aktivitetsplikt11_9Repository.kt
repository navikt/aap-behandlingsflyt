package no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt

import no.nav.aap.aktivitetsplikt.Aktivitetsplikt11_9Grunnlag
import no.nav.aap.aktivitetsplikt.Aktivitetsplikt11_9Vurdering
import no.nav.aap.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository

interface Aktivitetsplikt11_9Repository : Repository {
    fun hentHvisEksisterer(behandlingId: BehandlingId): Aktivitetsplikt11_9Grunnlag?
    fun lagre(behandlingId: BehandlingId, vurderinger: Set<Aktivitetsplikt11_9Vurdering>)
}