package no.nav.aap.behandlingsflyt.pip

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.pip.IdentPåSak.Companion.filterDistinctIdent
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.SakPathParam
import no.nav.aap.tilgang.authorizedGet
import javax.sql.DataSource

fun NormalOpenAPIRoute.behandlingsflytPipApi(dataSource: DataSource, repositoryRegistry: RepositoryRegistry) {
    route("/pip/api") {
        route("/sak/{saksnummer}/identer") {
            authorizedGet<SakDTO, IdenterDTO>(
                AuthorizationParamPathConfig(
                    sakPathParam = SakPathParam("saksnummer"),
                    applicationRole = "pip-api",
                    applicationsOnly = true
                )
            ) { req ->
                val saksnummer = req.saksnummer
                val identer = dataSource.transaction(readOnly = true) { connection ->
                    PipService(repositoryRegistry.provider(connection))
                        .finnIdenterPåSak(Saksnummer(saksnummer))
                }
                respond(
                    IdenterDTO(
                        søker = identer.filterDistinctIdent(IdentPåSak.Opprinnelse.PERSON),
                        barn = identer.filterDistinctIdent(IdentPåSak.Opprinnelse.BARN)
                    )
                )
            }
        }

        route("/behandling/{behandlingsnummer}/identer") {
            authorizedGet<BehandlingDTO, IdenterDTO>(
                AuthorizationParamPathConfig(
                    behandlingPathParam = BehandlingPathParam("behandlingsnummer"),
                    applicationRole = "pip-api",
                    applicationsOnly = true
                )
            ) { req ->
                val behandlingsnummer = req.behandlingsnummer
                val identer = dataSource.transaction(readOnly = true) { connection ->
                    PipService(repositoryRegistry.provider(connection))
                        .finnIdenterPåBehandling(BehandlingReferanse(behandlingsnummer))
                }
                respond(
                    IdenterDTO(
                        søker = identer.filterDistinctIdent(IdentPåSak.Opprinnelse.PERSON),
                        barn = identer.filterDistinctIdent(IdentPåSak.Opprinnelse.BARN)
                    )
                )
            }
        }
    }
}
