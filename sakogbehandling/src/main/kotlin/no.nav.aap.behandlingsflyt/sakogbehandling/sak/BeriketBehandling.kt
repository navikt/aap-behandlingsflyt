package no.nav.aap.behandlingsflyt.sakogbehandling.sak

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.verdityper.sakogbehandling.TypeBehandling
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

data class BeriketBehandling(
    val behandling: Behandling,
    val tilstand: BehandlingTilstand,
    val sisteAvsluttedeBehandling: BehandlingId?
) {
    fun skalKopierFraSisteBehandling(): Boolean {
        return tilstand == BehandlingTilstand.NY && behandling.typeBehandling() == TypeBehandling.Revurdering && sisteAvsluttedeBehandling != null
    }
}

enum class BehandlingTilstand {
    EKSISTERENDE, NY
}
