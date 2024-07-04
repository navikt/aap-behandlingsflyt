package no.nav.aap.behandlingsflyt.flyt.flate.visning

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.verdityper.flyt.StegGruppe
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

class StartStegVisningUtleder(connection: DBConnection) : StegGruppeVisningUtleder {

    override fun skalVises(behandlingId: BehandlingId): Boolean {
        return false
    }

    override fun gruppe(): StegGruppe {
        return StegGruppe.START_BEHANDLING
    }

}