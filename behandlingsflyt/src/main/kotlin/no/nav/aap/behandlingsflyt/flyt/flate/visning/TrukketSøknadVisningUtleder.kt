package no.nav.aap.behandlingsflyt.flyt.flate.visning

import no.nav.aap.behandlingsflyt.kontrakt.steg.StegGruppe
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.ÅrsakTilBehandling
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.RepositoryProvider

class TrukketSøknadVisningUtleder(
    connection: DBConnection,
) : StegGruppeVisningUtleder {
    private val behandlingRepository = RepositoryProvider(connection).provide<BehandlingRepository>()

    override fun skalVises(behandlingId: BehandlingId): Boolean {
        val behandling = behandlingRepository.hent(behandlingId)
        return behandling.årsaker().any {
            it.type == ÅrsakTilBehandling.SØKNAD_TRUKKET
        }
    }

    override fun gruppe(): StegGruppe {
        return StegGruppe.SØKNAD
    }
}