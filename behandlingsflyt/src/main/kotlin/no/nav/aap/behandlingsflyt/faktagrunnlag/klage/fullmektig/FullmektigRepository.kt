package no.nav.aap.behandlingsflyt.faktagrunnlag.klage.fullmektig

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository

interface FullmektigRepository: Repository {
    fun hentHvisEksisterer(behandlingId: BehandlingId): FullmektigGrunnlag?
    fun lagre(behandlingId: BehandlingId, vurdering: FullmektigVurdering)
}