package no.nav.aap.behandlingsflyt.steg.samordning.refusjonskrav

import no.nav.aap.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.lookup.repository.Repository
import no.nav.aap.samordning.refusjonskrav.TjenestepensjonRefusjonskravVurdering

interface TjenestepensjonRefusjonsKravVurderingRepository : Repository {
    fun hentHvisEksisterer(behandlingId: BehandlingId): TjenestepensjonRefusjonskravVurdering?
    fun hent(behandlingId: BehandlingId): TjenestepensjonRefusjonskravVurdering
    fun lagre(sakId: SakId, behandlingId: BehandlingId, vurdering: TjenestepensjonRefusjonskravVurdering)
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}