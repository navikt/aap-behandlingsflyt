package no.nav.aap.behandlingsflyt.sakogbehandling.sak

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.EndringType
import no.nav.aap.verdityper.sakogbehandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.behandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Årsak

class SakOgBehandlingService(connection: DBConnection) {

    private val sakRepository = sakRepository(connection)
    private val behandlingRepository = behandlingRepository(connection)

    fun finnEllerOpprettBehandling(key: Saksnummer): BeriketBehandling {
        val sak = sakRepository.hent(key)

        val sisteBehandlingForSak = behandlingRepository.finnSisteBehandlingFor(sak.id)

        if (sisteBehandlingForSak == null) {
            return BeriketBehandling(
                behandling = behandlingRepository.opprettBehandling(
                    sak.id,
                    listOf(Årsak(EndringType.MOTTATT_SØKNAD)),
                    TypeBehandling.Førstegangsbehandling
                ), tilstand = BehandlingTilstand.NY, sisteAvsluttedeBehandling = null
            )

        } else {
            if (sisteBehandlingForSak.status().erAvsluttet()) {
                val nyBehandling = behandlingRepository.opprettBehandling(
                    sak.id,
                    listOf(Årsak(EndringType.MOTTATT_SØKNAD)),
                    TypeBehandling.Revurdering
                )

                return BeriketBehandling(
                    behandling = nyBehandling,
                    tilstand = BehandlingTilstand.NY,
                    sisteAvsluttedeBehandling = sisteBehandlingForSak.id
                )

            } else {
                return BeriketBehandling(
                    behandling = sisteBehandlingForSak,
                    tilstand = BehandlingTilstand.EKSISTERENDE,
                    sisteAvsluttedeBehandling = null
                )
            }
        }
    }
}