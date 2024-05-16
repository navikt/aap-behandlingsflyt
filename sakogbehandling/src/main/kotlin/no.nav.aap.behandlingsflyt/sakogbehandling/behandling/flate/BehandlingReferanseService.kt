package no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.verdityper.feilhåndtering.ElementNotFoundException
import java.util.*

class BehandlingReferanseService(val connection: DBConnection) {

    fun behandling(behandlingReferanse: BehandlingReferanse): Behandling {
        val eksternReferanse: UUID
        try {
            eksternReferanse = behandlingReferanse.ref()
        } catch (exception: IllegalArgumentException) {
            throw ElementNotFoundException()
        }

        return BehandlingRepositoryImpl(connection).hent(eksternReferanse)
    }
}