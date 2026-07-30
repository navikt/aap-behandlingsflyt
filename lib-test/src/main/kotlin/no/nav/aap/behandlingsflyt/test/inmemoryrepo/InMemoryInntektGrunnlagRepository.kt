package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.behandling.beregning.Månedsinntekt
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektGrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektPerÅr
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId

object InMemoryInntektGrunnlagRepository : InntektGrunnlagRepository {

    private val memory = HashMap<BehandlingId, InntektGrunnlag>()
    private val lock = Any()

    override fun lagre(
        behandlingId: BehandlingId,
        inntekter: Set<InntektPerÅr>,
        inntektPerMåned: Set<Månedsinntekt>
    ) {
        synchronized(lock) {
            memory[behandlingId] = InntektGrunnlag(inntekter = inntekter, inntektPerMåned = inntektPerMåned)
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

    override fun hentHvisEksisterer(behandlingId: BehandlingId): InntektGrunnlag? {
        synchronized(lock) {
            return memory[behandlingId]
        }
    }

    override fun hent(behandlingId: BehandlingId): InntektGrunnlag {
        return requireNotNull(hentHvisEksisterer(behandlingId)) { "Fant ikke inntektgrunnlag for behandlingId=$behandlingId." }
    }

    override fun slett(behandlingId: BehandlingId) {
        synchronized(lock) {
            memory.remove(behandlingId)
        }
    }
}
