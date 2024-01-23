package no.nav.aap.behandlingsflyt.flyt.behandlingstyper

import no.nav.aap.behandlingsflyt.flyt.BehandlingType
import no.nav.aap.behandlingsflyt.flyt.BehandlingFlyt

object Revurdering : BehandlingType {
    override fun flyt(): BehandlingFlyt {
        //TODO: Må sørge for å kopiere Faktagrunnlag fra forrige behandling til ny behandling
        //grunnlagKopierer.overfør(sisteBehandlingForSak.id, nyBehandling.id)
        return Førstegangsbehandling.flyt() // Returnerer bare samme fly atm
    }

    override fun identifikator(): String {
        return "ae0028"
    }
}