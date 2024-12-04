package no.nav.aap.behandlingsflyt.flyt.steg.internal

import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegKonstruktør
import no.nav.aap.komponenter.dbconnect.DBConnection

class StegKonstruktørImpl(private val connection: DBConnection) : StegKonstruktør {
    override fun konstruer(steg: FlytSteg): BehandlingSteg {
        return steg.konstruer(connection)
    }

    override fun markerSavepoint() {
        connection.markerSavepoint()
    }
}