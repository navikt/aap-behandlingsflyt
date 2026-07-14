package no.nav.aap.behandlingsflyt

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.dokumentinnhenting.KandidatForPurringRepository
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.tilgang.AuthorizationBodyPathConfig
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.authorizedGet
import javax.sql.DataSource

fun NormalOpenAPIRoute.purringApi(
    dataSource: DataSource, repositoryRegistry: RepositoryRegistry
) {
    route("/api/dokumentinnhenting/purring") {
        authorizedGet<Unit, List<BehandlingReferanse>>(
            AuthorizationBodyPathConfig(
                applicationRole = "finn-kandidater-for-purring",
                applicationsOnly = true,
                operasjon = Operasjon.SAKSBEHANDLE,
            )
        ) { _ ->
            val behandlingsreferanser = dataSource.transaction(readOnly = true) { connection ->
                val repositoryProvider = repositoryRegistry.provider(connection)
                val kandidatForPurringRepository = repositoryProvider.provide<KandidatForPurringRepository>()

                kandidatForPurringRepository.finnKandidaterForPurring()
            }
            respond(behandlingsreferanser)
        }
    }
}