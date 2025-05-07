package no.nav.aap.behandlingsflyt.faktagrunnlag

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.lookup.repository.RepositoryRegistry

interface GrunnlagKopierer {
    fun overfør(fraBehandlingId: BehandlingId, tilBehandlingId: BehandlingId)

    companion object {
        /* TODO: Flytt GrunnlagKopiererImpl til repository-modulen og bruk RepositoryProvider. */
        operator fun invoke(connection: DBConnection): GrunnlagKopierer {
            return GrunnlagKopiererImpl(RepositoryRegistry.provider(connection))
        }
    }
}

/**
 * Har som ansvar å sette i stand en behandling etter opprettelse
 *
 * Knytter alle opplysninger fra forrige til den nye i en immutable state
 */
class GrunnlagKopiererImpl(
    private val repositoryProvider: RepositoryProvider,
) : GrunnlagKopierer {
    override fun overfør(fraBehandlingId: BehandlingId, tilBehandlingId: BehandlingId) {
        require(fraBehandlingId != tilBehandlingId)

        repositoryProvider.provideAlle().forEach { repository ->
            if (repository is no.nav.aap.lookup.repository.Repository) {
                repository.kopier(fraBehandlingId, tilBehandlingId)
            }
        }
    }
}
