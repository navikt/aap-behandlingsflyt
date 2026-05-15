package no.nav.aap.behandlingsflyt.behandling.rettighet

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.underveis.RettighetstypeService
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate.SaksnummerParameter
import no.nav.aap.behandlingsflyt.tilgang.relevanteIdenterForSakResolver
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.SakPathParam
import no.nav.aap.tilgang.authorizedGet
import javax.sql.DataSource

fun NormalOpenAPIRoute.rettighetsinfoApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider
) {
    route("/api/sak/{saksnummer}/rettighetsinfo") {
        authorizedGet<SaksnummerParameter, RettighetsinfoDto>(
            AuthorizationParamPathConfig(
                relevanteIdenterResolver = relevanteIdenterForSakResolver(repositoryRegistry, dataSource),
                sakPathParam = SakPathParam("saksnummer")
            )
        ) { saksnummer ->
            val respons: RettighetsinfoDto = dataSource.transaction(readOnly = true) { connection ->
                val repositoryProvider = repositoryRegistry.provider(connection)
                val rettighetstypeService = RettighetstypeService(repositoryProvider, gatewayProvider)

                val sisteDagMedRett = rettighetstypeService.sisteDagMedRett(Saksnummer(saksnummer.saksnummer))

                RettighetsinfoDto(
                    sisteDagMedRett = sisteDagMedRett)
            }

            respond(respons)
        }
    }
}
