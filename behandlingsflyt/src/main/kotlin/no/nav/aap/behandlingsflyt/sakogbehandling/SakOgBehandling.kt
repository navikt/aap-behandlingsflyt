package no.nav.aap.behandlingsflyt.sakogbehandling

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId

data class SakOgBehandling(val sakId: SakId, val behandlingId: BehandlingId) {
    override fun toString(): String {
        return "SakOgBehandling(sakId=$sakId, behandlingId=$behandlingId)"
    }
}