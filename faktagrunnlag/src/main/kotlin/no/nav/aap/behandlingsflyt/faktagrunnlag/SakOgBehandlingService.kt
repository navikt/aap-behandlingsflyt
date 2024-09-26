package no.nav.aap.behandlingsflyt.faktagrunnlag

import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Årsak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.BehandlingTilstand
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.BeriketBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.SakRepositoryImpl
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

class SakOgBehandlingService(connection: DBConnection) {

    private val sakRepository = SakRepositoryImpl(connection)
    private val behandlingRepository = BehandlingRepositoryImpl(connection)
    private val grunnlagKopierer = GrunnlagKopierer(connection)

    fun finnEllerOpprettBehandling(key: Saksnummer, årsaker: List<Årsak>): BeriketBehandling {
        val sak = sakRepository.hent(key)

        val sisteBehandlingForSak = behandlingRepository.finnSisteBehandlingFor(sak.id)

        if (sisteBehandlingForSak == null) {
            return BeriketBehandling(
                behandling = behandlingRepository.opprettBehandling(
                    sak.id,
                    årsaker,
                    TypeBehandling.Førstegangsbehandling
                ), tilstand = BehandlingTilstand.NY, sisteAvsluttedeBehandling = null
            )

        } else {
            if (sisteBehandlingForSak.status().erAvsluttet()) {
                val nyBehandling = behandlingRepository.opprettBehandling(
                    sak.id,
                    årsaker,
                    TypeBehandling.Revurdering
                )


                val beriketBehandling = BeriketBehandling(
                    behandling = nyBehandling,
                    tilstand = BehandlingTilstand.NY,
                    sisteAvsluttedeBehandling = sisteBehandlingForSak.id
                )
                if (beriketBehandling.skalKopierFraSisteBehandling()) {
                    grunnlagKopierer.overfør(requireNotNull(beriketBehandling.sisteAvsluttedeBehandling), nyBehandling.id)
                }

                return beriketBehandling

            } else {
                // Oppdater årsaker hvis nødvendig
                behandlingRepository.oppdaterÅrsaker(sisteBehandlingForSak, årsaker)
                return BeriketBehandling(
                    behandling = sisteBehandlingForSak,
                    tilstand = BehandlingTilstand.EKSISTERENDE,
                    sisteAvsluttedeBehandling = null
                )
            }
        }
    }

    fun hentSakFor(behandlingId: BehandlingId): Sak {
        val behandling = behandlingRepository.hent(behandlingId)
        return sakRepository.hent(behandling.sakId)
    }
}