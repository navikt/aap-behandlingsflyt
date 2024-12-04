package no.nav.aap.behandlingsflyt.forretningsflyt.behandlingstyper

import no.nav.aap.behandlingsflyt.flyt.BehandlingFlyt
import no.nav.aap.behandlingsflyt.flyt.BehandlingType

object Førstegangsbehandling : BehandlingType {
    override fun flyt(): BehandlingFlyt {
        return Revurdering.flyt().utenÅrsaker()
    }
}