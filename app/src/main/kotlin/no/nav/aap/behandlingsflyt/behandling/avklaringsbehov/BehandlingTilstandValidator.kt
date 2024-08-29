package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.motor.FlytJobbRepository

class BehandlingTilstandValidator(connection: DBConnection) {
    private val flytJobbRepository = FlytJobbRepository(connection)
    private val behandlingReferanseService = BehandlingReferanseService(BehandlingRepositoryImpl(connection))

    fun validerTilstand(behandlingReferanse: BehandlingReferanse, behandlingVersjon: Long) {
        val behandling = behandlingReferanseService.behandling(behandlingReferanse)
        ValiderBehandlingTilstand.validerTilstandBehandling(behandling, behandlingVersjon)

        val jobberForBehandling = flytJobbRepository.hentJobberForBehandling(behandling.id.toLong())
        if (jobberForBehandling.isNotEmpty()) {
            throw BehandlingUnderProsesseringException()
        }
    }
}