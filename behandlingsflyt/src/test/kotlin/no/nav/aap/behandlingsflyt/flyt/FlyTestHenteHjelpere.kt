package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.repository.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.type.Periode
import javax.sql.DataSource

class FlyTestHenteHjelpere(private val dataSource: DataSource) {
    fun hentBehandling(sakId: SakId): Behandling {
        return dataSource.transaction(readOnly = true) { connection ->
            val finnSisteBehandlingFor = BehandlingRepositoryImpl(connection).finnSisteBehandlingFor(
                sakId,
                listOf(TypeBehandling.Førstegangsbehandling, TypeBehandling.Revurdering, TypeBehandling.Klage)
            )
            requireNotNull(finnSisteBehandlingFor)
        }
    }

    fun hentSak(ident: Ident, periode: Periode): Sak {
        return dataSource.transaction { connection ->
            SakRepositoryImpl(connection).finnEllerOpprett(
                PersonRepositoryImpl(connection).finnEllerOpprett(listOf(ident)),
                periode
            )
        }
    }

    fun hentAlleAvklaringsbehov(behandling: Behandling): List<Avklaringsbehov> {
        return dataSource.transaction(readOnly = true) {
            AvklaringsbehovRepositoryImpl(it).hentAvklaringsbehovene(
                behandling.id
            ).alle()
        }
    }
}