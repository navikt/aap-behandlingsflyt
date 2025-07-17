package no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate

import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.komponenter.httpklient.exception.VerdiIkkeFunnetException
import org.slf4j.LoggerFactory

class BehandlingReferanseService(private val behandlingRepository: BehandlingRepository) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun behandling(behandlingReferanse: BehandlingReferanse): Behandling {
        try {
            return behandlingRepository.hent(behandlingReferanse)
        } catch (e: NoSuchElementException) {
            log.info("Fant ikke behandling med ref $behandlingReferanse. Stacktrace: ${e.stackTraceToString()}")

            throw VerdiIkkeFunnetException(
                message = "Fant ikke behandling med ref $behandlingReferanse.",
            )
        }
    }
}