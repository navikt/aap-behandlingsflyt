package no.nav.aap.behandlingsflyt.behandling.tilkjentytelse

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.tilgang.relevanteIdenterForBehandlingResolver
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.authorizedGet
import javax.sql.DataSource

fun NormalOpenAPIRoute.tilkjentYtelseApi(dataSource: DataSource, repositoryRegistry: RepositoryRegistry) {
    route("/api/behandling") {
        route("/tilkjentV2/{referanse}") {
            authorizedGet<BehandlingReferanse, TilkjentYtelse2Dto>(
                AuthorizationParamPathConfig(
                    relevanteIdenterResolver = relevanteIdenterForBehandlingResolver(repositoryRegistry, dataSource),
                    operasjon = Operasjon.SE,
                    behandlingPathParam = BehandlingPathParam("referanse")
                )
            ) { behandlingreferanse ->
                val tilkjentYtelseDto =
                    TilkjentYtelseService(dataSource, repositoryRegistry)
                        .hentTilkjentYtelse(behandlingreferanse)

                respond(tilkjentYtelseDto)
            }
        }

    }
}