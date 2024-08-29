package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.verdityper.flyt.StegType

class GeneriskTestFlytSteg(private val stegType: StegType) : FlytSteg {
    override fun konstruer(connection: DBConnection): BehandlingSteg {
        return GeneriskTestSteg()
    }

    override fun type(): StegType {
        return stegType
    }
}
