package no.nav.aap.verdityper.sakogbehandling

class SakOgBehandling(val sakId: SakId, val behandlingId: BehandlingId) {
    override fun toString(): String {
        return "SakOgBehandling(sakId=$sakId, behandlingId=$behandlingId)"
    }
}