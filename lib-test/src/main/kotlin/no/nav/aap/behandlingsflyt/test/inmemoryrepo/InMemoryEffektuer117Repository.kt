package no.nav.aap.behandlingsflyt.test.inmemoryrepo

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.effektuer11_7.Effektuer11_7Forhåndsvarsel
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.effektuer11_7.Effektuer11_7Grunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.effektuer11_7.Effektuer11_7Repository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.effektuer11_7.Effektuer11_7Vurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import java.util.concurrent.ConcurrentHashMap

object InMemoryEffektuer117Repository: Effektuer11_7Repository {
    private val grunnlag = ConcurrentHashMap<BehandlingId, Effektuer11_7Grunnlag>()
    private val lock = Object()

    override fun lagreVurdering(behandlingId: BehandlingId, vurdering: Effektuer11_7Vurdering) {
        synchronized(lock) {
            grunnlag[behandlingId] = hentEllerOpprett(behandlingId)
                .copy(vurdering = vurdering)
        }
    }

    override fun lagreVarsel(
        behandlingId: BehandlingId,
        varsel: Effektuer11_7Forhåndsvarsel
    ) {
        synchronized(lock) {
            grunnlag[behandlingId] = hentEllerOpprett(behandlingId)
                .let {
                    it.copy(varslinger = it.varslinger + varsel)
                }
        }
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): Effektuer11_7Grunnlag? {
        synchronized(lock) {
            return grunnlag[behandlingId]
        }
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        TODO("Not yet implemented")
    }

    override fun slett(behandlingId: BehandlingId) {
    }

    private fun hentEllerOpprett(behandlingId: BehandlingId): Effektuer11_7Grunnlag {
        return grunnlag.computeIfAbsent(behandlingId) { Effektuer11_7Grunnlag(null, emptyList()) }
    }
}
