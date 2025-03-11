package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository

interface SamordningUføreRepository : Repository {
    fun hentHvisEksisterer(behandlingId: BehandlingId): VurderingerForSamordningUføre?
    fun lagre(behandlingId: BehandlingId, vurdering: VurderingerForSamordningUføre)
}