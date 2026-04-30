package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EtableringEgenVirksomhetGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EtableringEgenVirksomhetRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EtableringEgenVirksomhetVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId

object InMemoryEtableringEgenVirksomRepository : EtableringEgenVirksomhetRepository {
    private val mutex = Any()
    private val grunnlag = HashMap<BehandlingId, EtableringEgenVirksomhetGrunnlag>()

    override fun hentHvisEksisterer(behandlingId: BehandlingId) =
        synchronized(mutex) { grunnlag[behandlingId] }

    override fun lagre(
        behandlingId: BehandlingId,
        etableringEgenvirksomhetVurderinger: List<EtableringEgenVirksomhetVurdering>
    ) = synchronized(mutex) {
        grunnlag[behandlingId] = EtableringEgenVirksomhetGrunnlag(etableringEgenvirksomhetVurderinger)
    }

    override fun slett(behandlingId: BehandlingId) {
        synchronized(mutex) {
            grunnlag.remove(behandlingId)
        }
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
}