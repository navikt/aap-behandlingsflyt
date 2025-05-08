package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.samordning.refusjonskrav

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.lookup.repository.Repository

interface TjenestepensjonRefusjonsKravVurderingRepository : Repository {
    fun hentHvisEksisterer(behandlingId: BehandlingId): TjenestepensjonRefusjonskravVurdering?
    fun hent(behandlingId: BehandlingId): TjenestepensjonRefusjonskravVurdering
    fun lagre(sakId: SakId, behandlingId: BehandlingId, vurdering: TjenestepensjonRefusjonskravVurdering)
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}