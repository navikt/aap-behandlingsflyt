package no.nav.aap.behandlingsflyt.sakogbehandling.sak

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.EndringType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.behandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Årsak

class SakOgBehandlingService(connection: DBConnection) {

    private val sakRepository = sakRepository(connection)
    private val behandlingRepository = behandlingRepository(connection)

    fun finnEnRelevantBehandling(key: Saksnummer): Behandling {
        val sak = sakRepository.hent(key)

        val sisteBehandlingForSak = behandlingRepository.finnSisteBehandlingFor(sak.id)

        if (sisteBehandlingForSak == null) {
            return behandlingRepository.opprettBehandling(
                sak.id,
                listOf(Årsak(EndringType.MOTTATT_SØKNAD)),
                TypeBehandling.Førstegangsbehandling)

        } else {
            if (sisteBehandlingForSak.status().erAvsluttet()) {
                val nyBehandling = behandlingRepository.opprettBehandling(
                    sak.id,
                    listOf(Årsak(EndringType.MOTTATT_SØKNAD)),
                    TypeBehandling.Revurdering
                )

                return nyBehandling

            } else {
                return sisteBehandlingForSak
            }
        }
    }
}