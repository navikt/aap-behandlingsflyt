package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.misc.inntekt.ManuellInntektGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.ManuellInntektGrunnlagRepository
import no.nav.aap.beregning.ManuellInntektVurdering
import no.nav.aap.behandling.BehandlingId

object InMemoryManuellInntektGrunnlagRepository : ManuellInntektGrunnlagRepository {

    private val memory = HashMap<BehandlingId, ManuellInntektGrunnlag>()
    private val lock = Any()

    override fun lagre(behandlingId: BehandlingId, manuellVurderinger: Set<ManuellInntektVurdering>) {
        synchronized(lock) {
            memory[behandlingId] = ManuellInntektGrunnlag(manuelleInntekter = manuellVurderinger)
        }
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        synchronized(lock) {
            require(fraBehandling != tilBehandling)

            val grunnlag = memory[fraBehandling]
            if (grunnlag != null) {
                memory[tilBehandling] = grunnlag
            }
        }
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): ManuellInntektGrunnlag? {
        synchronized(lock) {
            return memory[behandlingId]
        }
    }

    override fun slett(behandlingId: BehandlingId) {
        synchronized(lock) {
            memory.remove(behandlingId)
        }
    }
}
