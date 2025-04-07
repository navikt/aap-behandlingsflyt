package no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate

import io.ktor.http.*
import no.nav.aap.behandlingsflyt.exception.UgyldigForespørselException
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import org.slf4j.LoggerFactory

class BehandlingReferanseService(private val behandlingRepositoryImpl: BehandlingRepository) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun behandling(behandlingReferanse: BehandlingReferanse): Behandling {
        try {
            return behandlingRepositoryImpl.hent(behandlingReferanse)
        } catch (e: NoSuchElementException) {
            log.info("Fant ikke behandling med ref $behandlingReferanse. Stacktrace: ${e.stackTraceToString()}")

            // TODO: Må ha egen exception for "NotFound", men det finnes allerede en IkkeFunnetException...
            throw UgyldigForespørselException(
                status = HttpStatusCode.NotFound,
                message = "Fant ikke behandling med ref $behandlingReferanse.",
            )
        }
    }
}