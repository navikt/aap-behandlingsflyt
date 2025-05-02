package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.refusjonkrav

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.lookup.repository.Repository

interface RefusjonkravRepository: Repository {
    fun hentHvisEksisterer(behandlingId: BehandlingId): RefusjonkravVurdering?
    fun hentAlleVurderingerPåSak(sakId: SakId): List<RefusjonkravVurdering>
    fun lagre(sakId: SakId, behandlingId: BehandlingId, refusjonkravVurderinger: RefusjonkravVurdering)
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId)
}