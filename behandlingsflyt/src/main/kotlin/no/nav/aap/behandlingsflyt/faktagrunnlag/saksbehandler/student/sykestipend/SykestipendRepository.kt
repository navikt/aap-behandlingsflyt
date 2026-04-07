package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.sykestipend

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository

interface SykestipendRepository: Repository {
    fun lagre(behandlingId: BehandlingId, vurdering: SykestipendVurdering)
    fun hentHvisEksisterer(behandlingId: BehandlingId): SykestipendGrunnlag?
    fun deaktiverGrunnlag(behandlingId: BehandlingId)
}