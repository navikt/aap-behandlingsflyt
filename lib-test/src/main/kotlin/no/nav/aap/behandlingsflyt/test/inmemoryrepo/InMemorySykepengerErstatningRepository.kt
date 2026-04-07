package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykepengerErstatningGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykepengerErstatningRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykepengerVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId

object InMemorySykepengerErstatningRepository: SykepengerErstatningRepository {
    private val mutex = Any()
    private val grunnlag = HashMap<BehandlingId, SykepengerErstatningGrunnlag>()

    override fun lagre(
        behandlingId: BehandlingId,
        vurderinger: List<SykepengerVurdering>
    ) = synchronized(mutex) {
        grunnlag[behandlingId] = SykepengerErstatningGrunnlag(vurderinger)
    }

    override fun kopier(
        fraBehandling: BehandlingId,
        tilBehandling: BehandlingId
    ) {
        synchronized(mutex) {
            val fraGrunnlag = grunnlag[fraBehandling]
            if (fraGrunnlag != null) {
                grunnlag[tilBehandling] = fraGrunnlag
            }
        }
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId) = synchronized(mutex) {
        grunnlag[behandlingId]
    }

    override fun slett(behandlingId: BehandlingId) {
        synchronized(mutex) {
            grunnlag.remove(behandlingId)
        }
    }
}