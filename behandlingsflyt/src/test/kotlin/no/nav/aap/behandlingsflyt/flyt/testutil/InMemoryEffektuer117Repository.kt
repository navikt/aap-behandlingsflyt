package no.nav.aap.behandlingsflyt.flyt.testutil

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.effektuer11_7.Effektuer11_7Forhåndsvarsel
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.effektuer11_7.Effektuer11_7Grunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.effektuer11_7.Effektuer11_7Repository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.effektuer11_7.Effektuer11_7Vurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId

object InMemoryEffektuer117Repository: Effektuer11_7Repository {
    private val grunnlag = mutableMapOf<BehandlingId, Effektuer11_7Grunnlag>()

    override fun lagreVurdering(behandlingId: BehandlingId, vurdering: Effektuer11_7Vurdering) {
        grunnlag[behandlingId] = hentEllerOpprett(behandlingId)
            .copy(vurdering = vurdering)
    }

    override fun lagreVarsel(
        behandlingId: BehandlingId,
        varsel: Effektuer11_7Forhåndsvarsel
    ) {
        grunnlag[behandlingId] = hentEllerOpprett(behandlingId)
            .let {
                it.copy(varslinger = it.varslinger + varsel)
            }
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): Effektuer11_7Grunnlag? {
        return grunnlag[behandlingId]
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        TODO("Not yet implemented")
    }

    private fun hentEllerOpprett(behandlingId: BehandlingId): Effektuer11_7Grunnlag {
        return grunnlag.computeIfAbsent(behandlingId) { Effektuer11_7Grunnlag(null, emptyList()) }
    }
}