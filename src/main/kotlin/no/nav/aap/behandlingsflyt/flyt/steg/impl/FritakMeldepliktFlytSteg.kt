package no.nav.aap.behandlingsflyt.flyt.steg.impl

import no.nav.aap.behandlingsflyt.behandling.BehandlingService
import no.nav.aap.behandlingsflyt.dbstuff.DBConnection
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegType

object FritakMeldepliktFlytSteg : FlytSteg {

    override fun konstruer(connection: DBConnection): BehandlingSteg {
        return FritakMeldepliktSteg(BehandlingService(connection))
    }

    override fun type(): StegType {
        return StegType.FRITAK_MELDEPLIKT
    }
}
