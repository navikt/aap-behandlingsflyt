package no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.verdityper.feilhåndtering.ElementNotFoundException
import java.util.*

class BehandlingReferanseService(private val behandlingRepositoryImpl: BehandlingRepository) {
    fun behandling(behandlingReferanse: BehandlingReferanse): Behandling {
        val eksternReferanse: UUID
        try {
            eksternReferanse = behandlingReferanse.ref()
        } catch (exception: IllegalArgumentException) {
            throw ElementNotFoundException()
        }

        try {
            return behandlingRepositoryImpl.hent(eksternReferanse)
        } catch (e: NoSuchElementException) {
            throw ElementNotFoundException()
        }
    }
}