package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.OvergangUføreGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.OvergangUføreRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.OvergangUføreVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId

object InMemoryOvergangUføreRepository: OvergangUføreRepository {
    private val mutex = Any()
    private val grunnlag = HashMap<BehandlingId, OvergangUføreGrunnlag>()

    override fun hentHvisEksisterer(behandlingId: BehandlingId) = synchronized(mutex) {
        grunnlag[behandlingId]
    }

    override fun hentHistoriskeOvergangUforeVurderinger(
        sakId: SakId,
        behandlingId: BehandlingId
    ) = emptyList<OvergangUføreVurdering>()

    override fun lagre(
        behandlingId: BehandlingId,
        overgangUføreVurderinger: List<OvergangUføreVurdering>
    )  = synchronized(mutex) {
        grunnlag[behandlingId] = OvergangUføreGrunnlag(overgangUføreVurderinger)
    }

    override fun kopier(
        fraBehandling: BehandlingId,
        tilBehandling: BehandlingId
    ) = synchronized(mutex) {
        val fraGrunnlag = grunnlag[fraBehandling]
        if (fraGrunnlag != null) {
            grunnlag[tilBehandling] = fraGrunnlag
        }
    }

    override fun slett(behandlingId: BehandlingId) {
        synchronized(mutex) {
            grunnlag.remove(behandlingId)
        }
    }
}