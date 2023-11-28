package no.nav.aap.behandlingsflyt.flyt.steg.impl

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.flyt.steg.StegType

class FritakMeldepliktSteg private constructor() : BehandlingSteg {

    override fun utfør(kontekst: FlytKontekst): StegResultat {
        return StegResultat()
    }

    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            return FritakMeldepliktSteg()
        }

        override fun type(): StegType {
            return StegType.FRITAK_MELDEPLIKT
        }
    }
}
