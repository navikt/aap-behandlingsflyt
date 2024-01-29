package no.nav.aap.behandlingsflyt.forretningsflyt.behandlingstyper

import no.nav.aap.behandlingsflyt.flyt.BehandlingType
import no.nav.aap.behandlingsflyt.flyt.BehandlingFlyt

object Revurdering : BehandlingType {
    override fun flyt(): BehandlingFlyt {
        return Førstegangsbehandling.flyt() // Returnerer bare samme fly atm
    }

}