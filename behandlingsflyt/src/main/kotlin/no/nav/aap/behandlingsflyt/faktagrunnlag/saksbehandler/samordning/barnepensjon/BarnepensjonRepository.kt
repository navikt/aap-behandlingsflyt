package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.samordning.barnepensjon

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.lookup.repository.Repository

interface BarnepensjonRepository : Repository {
    fun lagre(behandlingId: BehandlingId, vurdering: BarnepensjonVurdering)
    fun hentHvisEksisterer(behandlingId: BehandlingId): BarnepensjonGrunnlag?
    fun hentHistoriskeVurderinger(sakId: SakId, behandlingId: BehandlingId): List<BarnepensjonGrunnlag>
    fun deaktiverGrunnlag(behandlingId: BehandlingId)
}
