package no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.avbrytaktivitetspliktbehandling

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository

interface AvbrytAktivitetspliktbehandlingRepository : Repository {
    fun lagre(behandlingId: BehandlingId, vurdering: AvbrytAktivitetspliktbehandlingVurdering)
    fun hentHvisEksisterer(behandlingId: BehandlingId): AvbrytAktivitetspliktbehandlingGrunnlag?
}