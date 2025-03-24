package no.nav.aap.behandlingsflyt.faktagrunnlag

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.RepositoryProvider

interface GrunnlagKopierer {
    fun overfør(fraBehandlingId: BehandlingId, tilBehandlingId: BehandlingId)

    companion object {
        /* TODO: Flytt GrunnlagKopiererImpl til repository-modulen og bruk RepositoryProvider. */
        operator fun invoke(connection: DBConnection): GrunnlagKopierer {
            return GrunnlagKopiererImpl(connection)
        }
    }
}

/**
 * Har som ansvar å sette i stand en behandling etter opprettelse
 *
 * Knytter alle opplysninger fra forrige til den nye i en immutable state
 */
class GrunnlagKopiererImpl(connection: DBConnection): GrunnlagKopierer {

    private val repositoryProvider = RepositoryProvider(connection)

    override fun overfør(fraBehandlingId: BehandlingId, tilBehandlingId: BehandlingId) {
        require(fraBehandlingId != tilBehandlingId)

        repositoryProvider.provideAlle().forEach { repository -> repository.kopier(fraBehandlingId, tilBehandlingId) }
    }
}
