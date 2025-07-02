package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.refusjonkrav.RefusjonkravRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.refusjonkrav.RefusjonkravVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId

object InMemoryRefusjonKravRepository : RefusjonkravRepository {
    override fun hentHvisEksisterer(behandlingId: BehandlingId): List<RefusjonkravVurdering>?
    {
        return null
    }

    override fun hentAlleVurderingerPåSak(sakId: SakId): List<RefusjonkravVurdering>
    {
        return emptyList()
    }

    override fun lagre(sakId: SakId, behandlingId: BehandlingId, refusjonkravVurderinger: List<RefusjonkravVurdering>)
    {}

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
    {}

    override fun slett(behandling: BehandlingId)
    {}
}
