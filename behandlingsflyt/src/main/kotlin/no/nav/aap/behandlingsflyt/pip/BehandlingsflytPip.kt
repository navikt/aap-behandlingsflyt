package no.nav.aap.behandlingsflyt.pip

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.pip.PipRepository.IdentPåSak.Companion.filterDistinctIdent
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.SakPathParam
import no.nav.aap.tilgang.authorizedGet
import javax.sql.DataSource

fun NormalOpenAPIRoute.behandlingsflytPip(dataSource: DataSource) {
    val tilgangAzp = requiredConfigForKey("integrasjon.tilgang.azp")
    route("/pip/api") {
        route("/sak/{saksnummer}/identer") {
            authorizedGet<SakDTO, IdenterDTO>(
                AuthorizationParamPathConfig(
                    sakPathParam = SakPathParam("saksnummer"),
                    approvedApplications = setOf(tilgangAzp),
                    applicationsOnly = true
                )
            ) { req ->
                val saksnummer = req.saksnummer
                val identer = dataSource.transaction(readOnly = true) { connection ->
                    PipRepository(connection).finnIdenterPåSak(Saksnummer(saksnummer))
                }
                respond(
                    IdenterDTO(
                        søker = identer.filterDistinctIdent(PipRepository.IdentPåSak.Opprinnelse.PERSON),
                        barn = identer.filterDistinctIdent(PipRepository.IdentPåSak.Opprinnelse.BARN)
                    )
                )
            }
        }

        route("/behandling/{behandlingsnummer}/identer") {
            authorizedGet<BehandlingDTO, IdenterDTO>(
                AuthorizationParamPathConfig(
                    behandlingPathParam = BehandlingPathParam("behandlingsnummer"),
                    approvedApplications = setOf(tilgangAzp),
                    applicationsOnly = true
                )
            ) { req ->
                val behandlingsnummer = req.behandlingsnummer
                val identer = dataSource.transaction(readOnly = true) { connection ->
                    PipRepository(connection).finnIdenterPåBehandling(BehandlingReferanse(behandlingsnummer))
                }
                respond(
                    IdenterDTO(
                        søker = identer.filterDistinctIdent(PipRepository.IdentPåSak.Opprinnelse.PERSON),
                        barn = identer.filterDistinctIdent(PipRepository.IdentPåSak.Opprinnelse.BARN)
                    )
                )
            }
        }
    }
}
