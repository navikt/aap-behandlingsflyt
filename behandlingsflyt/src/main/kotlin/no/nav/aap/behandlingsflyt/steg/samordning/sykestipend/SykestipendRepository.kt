package no.nav.aap.behandlingsflyt.steg.samordning.sykestipend

import no.nav.aap.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository
import no.nav.aap.student.sykestipend.SykestipendGrunnlag
import no.nav.aap.student.sykestipend.SykestipendVurdering

interface SykestipendRepository: Repository {
    fun lagre(behandlingId: BehandlingId, vurdering: SykestipendVurdering)
    fun hentHvisEksisterer(behandlingId: BehandlingId): SykestipendGrunnlag?
    fun deaktiverGrunnlag(behandlingId: BehandlingId)
}