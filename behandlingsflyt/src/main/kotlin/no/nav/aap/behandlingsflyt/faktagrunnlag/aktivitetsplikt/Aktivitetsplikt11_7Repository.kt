package no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository

interface Aktivitetsplikt11_7Repository : Repository {
    fun hentHvisEksisterer(behandlingId: BehandlingId): Aktivitetsplikt11_7Grunnlag?
    fun hentAlleVurderinger(behandlingId: BehandlingId): Set<Aktivitetsplikt11_7Vurdering>
    fun lagre(behandlingId: BehandlingId, vurdering: Aktivitetsplikt11_7Vurdering)
}