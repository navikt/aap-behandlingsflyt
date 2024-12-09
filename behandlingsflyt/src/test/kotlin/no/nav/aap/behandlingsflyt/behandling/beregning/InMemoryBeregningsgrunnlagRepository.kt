package no.nav.aap.behandlingsflyt.behandling.beregning

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Beregningsgrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.BeregningsgrunnlagRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.util.concurrent.atomic.AtomicLong

object InMemoryBeregningsgrunnlagRepository : BeregningsgrunnlagRepository {

    private val idSeq = AtomicLong(10000)
    private val memory = HashMap<BehandlingId, Beregningsgrunnlag>()
    private val lock = Object()

    override fun hentHvisEksisterer(behandlingId: BehandlingId): Beregningsgrunnlag? {
        synchronized(lock) {
            return memory[behandlingId]
        }
    }

    override fun lagre(behandlingId: BehandlingId, beregningsgrunnlag: Beregningsgrunnlag) {
        synchronized(lock) {
            val id = BehandlingId(idSeq.andIncrement)
            if (memory.containsKey(id)) {
                throw IllegalArgumentException("Behandling id finnes allerede $id")
            }


            memory[id] = beregningsgrunnlag
        }
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        synchronized(lock) {
            require(fraBehandling != tilBehandling)

            val id = BehandlingId(idSeq.andIncrement)
            if (memory.containsKey(id)) {
                throw IllegalArgumentException("Behandling id finnes allerede $id")
            }

            memory[id] = memory[fraBehandling]!!
        }
    }

    override fun deaktiver(behandlingId: BehandlingId) {
        synchronized(lock) {
            memory.remove(behandlingId)
        }
    }
}