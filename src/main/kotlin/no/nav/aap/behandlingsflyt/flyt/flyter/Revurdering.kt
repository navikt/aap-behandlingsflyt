package no.nav.aap.behandlingsflyt.domene.behandling

import no.nav.aap.behandlingsflyt.flyt.BehandlingFlyt

object Revurdering : BehandlingType {
    override fun flyt(): BehandlingFlyt {
        return Førstegangsbehandling.flyt() // Returnerer bare samme fly atm
    }

    override fun identifikator(): String {
        return "ae0028"
    }
}