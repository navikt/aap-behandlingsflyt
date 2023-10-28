package no.nav.aap.behandlingsflyt.flyt.steg.impl

import no.nav.aap.behandlingsflyt.dbstuff.DBConnection
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegType

class GeneriskPlaceholderFlytSteg(private val stegType: StegType) : FlytSteg {
    override fun konstruer(connection: DBConnection): BehandlingSteg {
        return GeneriskPlaceholderSteg()
    }

    override fun type(): StegType {
        return stegType
    }
}
