package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov

import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.motor.FlytJobbRepository

class BehandlingTilstandValidator(
    private val behandlingReferanseService: BehandlingReferanseService,
    private val flytJobbRepository: FlytJobbRepository
) {

    fun validerTilstand(behandlingReferanse: BehandlingReferanse, behandlingVersjon: Long) {
        val behandling = behandlingReferanseService.behandling(behandlingReferanse)
        ValiderBehandlingTilstand.validerTilstandBehandling(behandling, behandlingVersjon)

        val jobberForBehandling = flytJobbRepository.hentJobberForBehandling(behandling.id.toLong())
        if (jobberForBehandling.isNotEmpty()) {
            throw BehandlingUnderProsesseringException()
        }
    }
}