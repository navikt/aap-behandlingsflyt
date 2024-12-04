package no.nav.aap.behandlingsflyt.flyt.steg

import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.komponenter.dbconnect.DBConnection

interface FlytSteg {
    fun konstruer(connection: DBConnection): BehandlingSteg

    fun type(): StegType
}
