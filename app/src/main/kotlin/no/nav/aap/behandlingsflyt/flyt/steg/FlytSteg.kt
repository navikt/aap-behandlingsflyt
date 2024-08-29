package no.nav.aap.behandlingsflyt.flyt.steg

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.verdityper.flyt.StegType

interface FlytSteg {
    fun konstruer(connection: DBConnection): BehandlingSteg

    fun type(): StegType
}
