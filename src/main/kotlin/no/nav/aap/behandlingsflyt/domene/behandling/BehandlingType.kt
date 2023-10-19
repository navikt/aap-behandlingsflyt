package no.nav.aap.behandlingsflyt.domene.behandling

import no.nav.aap.behandlingsflyt.flyt.BehandlingFlyt

interface BehandlingType {
    fun flyt(): BehandlingFlyt
    fun identifikator(): String
}

