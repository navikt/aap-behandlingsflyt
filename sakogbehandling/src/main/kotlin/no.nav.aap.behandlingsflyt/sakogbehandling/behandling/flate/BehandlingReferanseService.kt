package no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate

import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(BehandlingReferanseService::class.java)

class BehandlingReferanseService(private val behandlingRepositoryImpl: BehandlingRepository) {
    fun behandling(behandlingReferanse: BehandlingReferanse): Behandling {
        try {
            return behandlingRepositoryImpl.hent(behandlingReferanse)
        } catch (e: NoSuchElementException) {
            logger.info("Fant ikke behandling med ref $behandlingReferanse. Stacktrace: ${e.stackTraceToString()}")
            throw ElementNotFoundException()
        }
    }
}