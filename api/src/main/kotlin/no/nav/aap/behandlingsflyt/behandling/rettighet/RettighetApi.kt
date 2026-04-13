package no.nav.aap.behandlingsflyt.behandling.rettighet

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate.SaksnummerParameter
import no.nav.aap.behandlingsflyt.tilgang.relevanteIdenterForSakResolver
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.SakPathParam
import no.nav.aap.tilgang.authorizedGet
import javax.sql.DataSource

// TODO: Slett når vi har fjernet i frontend
fun NormalOpenAPIRoute.rettighetApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry
) {
    route("/api/sak/{saksnummer}/rettighet") {
        authorizedGet<SaksnummerParameter, List<Unit>>(
            AuthorizationParamPathConfig(
                relevanteIdenterResolver = relevanteIdenterForSakResolver(repositoryRegistry, dataSource),
                sakPathParam = SakPathParam("saksnummer")
            )
        ) {
            respond(emptyList())
        }
    }
}