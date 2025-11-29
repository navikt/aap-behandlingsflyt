package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Beregningsgrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.BeregningsgrunnlagRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId

object InMemoryBeregningsgrunnlagRepository : BeregningsgrunnlagRepository {

    private val memory = HashMap<BehandlingId, Beregningsgrunnlag>()
    private val lock = Object()

    override fun hentHvisEksisterer(behandlingId: BehandlingId): Beregningsgrunnlag? {
        synchronized(lock) {
            return memory[behandlingId]
        }
    }

    override fun lagre(behandlingId: BehandlingId, beregningsgrunnlag: Beregningsgrunnlag) {
        synchronized(lock) {
            memory[behandlingId] = beregningsgrunnlag
        }
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        synchronized(lock) {
            require(fraBehandling != tilBehandling)

            val beregningsgrunnlag = memory[fraBehandling]
            if (beregningsgrunnlag != null) {
                memory[tilBehandling] = beregningsgrunnlag
            }
        }
    }

    override fun deaktiver(behandlingId: BehandlingId) {
        synchronized(lock) {
            memory.remove(behandlingId)
        }
    }

    override fun slett(behandlingId: BehandlingId) {
    }
}
