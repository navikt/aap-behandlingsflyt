package no.nav.aap.behandlingsflyt.flyt.steg

import no.nav.aap.behandlingsflyt.dbstuff.DBConnection

interface FlytSteg {
    fun konstruer(connection: DBConnection): BehandlingSteg

    fun type(): StegType
}
