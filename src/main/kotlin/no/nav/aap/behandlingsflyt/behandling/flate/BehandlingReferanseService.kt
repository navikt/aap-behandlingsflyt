package no.nav.aap.behandlingsflyt.behandling.flate

import no.nav.aap.behandlingsflyt.ElementNotFoundException
import no.nav.aap.behandlingsflyt.behandling.Behandling
import no.nav.aap.behandlingsflyt.behandling.behandlingRepository
import no.nav.aap.behandlingsflyt.behandling.flate.BehandlingReferanse
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import java.util.*

class BehandlingReferanseService(val connection: DBConnection) {

    fun behandling(req: BehandlingReferanse): Behandling {
        val eksternReferanse: UUID
        try {
            eksternReferanse = req.ref()
        } catch (exception: IllegalArgumentException) {
            throw ElementNotFoundException()
        }

        return behandlingRepository(connection).hent(eksternReferanse)
    }
}