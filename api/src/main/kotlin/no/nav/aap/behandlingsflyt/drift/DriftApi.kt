package no.nav.aap.behandlingsflyt.drift

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respondWithStatus
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.authorizedPost
import javax.sql.DataSource

/**
 * API for å utføre manuelle operasjoner med forsøk på å rette opp i låste saker av varierende grunn.
 * Ikke bruk dette ved mindre du vet hva du gjør.
 * Med tid kan vi ha et admin-verktøy for alle disse.
 * */

// TODO: Denne resetter en behandling til start, men trenger å fikses til å kunne sette behandlingen i gang igjen.
fun NormalOpenAPIRoute.driftAPI(dataSource: DataSource, repositoryRegistry: RepositoryRegistry) {
    route("/api/drift") {
        route("/flyttbehandlingtilstart/{referanse}") {
            authorizedPost<BehandlingReferanse, Unit, Unit>(
                AuthorizationParamPathConfig(
                    behandlingPathParam = BehandlingPathParam("referanse"),
                    operasjon = Operasjon.DRIFTE
                )
            ){ req, _ ->
                dataSource.transaction { connection ->
                    val repositoryProvider = repositoryRegistry.provider(connection)
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                    val behandling = behandlingRepository.hent(BehandlingReferanse(req.referanse))

                    Driftfunksjoner(repositoryRegistry).flyttBehandlingTilStart(behandling.id, connection)
                }
                respondWithStatus(HttpStatusCode.Accepted)
            }
        }
    }
}