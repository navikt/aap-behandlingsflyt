package no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.lookup.repository.Repository

interface LovvalgDelvurderingRepository : Repository {
    fun hentHvisEksisterer(behandlingId: BehandlingId): LovvalgDelvurderingerGrunnlag?
    fun lagre(behandlingId: BehandlingId, vurdering: KanBehandlesAutomatiskVurdering)
    fun hentBasertPåDelvurderingId(delvurderingId: Long): LovvalgDelvurderingerGrunnlag?
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}
