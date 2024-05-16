package no.nav.aap.behandlingsflyt.avklaringsbehov

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.motor.OppgaveRepository

class BehandlingTilstandValidator(connection: DBConnection) {
    private val oppgaveRepository = OppgaveRepository(connection)
    private val behandlingReferanseService = BehandlingReferanseService(connection)

    fun validerTilstand(behandlingReferanse: BehandlingReferanse, behandlingVersjon: Long) {
        val behandling = behandlingReferanseService.behandling(behandlingReferanse)
        ValiderBehandlingTilstand.validerTilstandBehandling(behandling, behandlingVersjon)

        val oppgaveForBehandling = oppgaveRepository.hentOppgaveForBehandling(behandling.id)
        if (oppgaveForBehandling.isNotEmpty()) {
            throw BehandlingUnderProsesseringException()
        }
    }
}