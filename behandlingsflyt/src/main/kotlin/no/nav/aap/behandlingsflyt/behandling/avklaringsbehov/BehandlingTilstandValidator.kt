package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov

import no.nav.aap.behandlingsflyt.exception.BehandlingUnderProsesseringException
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.motor.FlytJobbRepository
import org.slf4j.LoggerFactory

class BehandlingTilstandValidator(
    private val behandlingReferanseService: BehandlingReferanseService,
    private val flytJobbRepository: FlytJobbRepository
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun validerTilstand(behandlingReferanse: BehandlingReferanse, behandlingVersjon: Long) {
        val behandling = behandlingReferanseService.behandling(behandlingReferanse)
        log.info("Validerer tilstand for behandling ${behandling.id}")
        ValiderBehandlingTilstand.validerTilstandBehandling(behandling, behandlingVersjon)

        val jobberForBehandling = flytJobbRepository.hentJobberForBehandling(behandling.id.toLong())
        if (jobberForBehandling.isNotEmpty()) {
            throw BehandlingUnderProsesseringException()
        }
    }
}