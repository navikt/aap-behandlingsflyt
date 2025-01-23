package no.nav.aap.behandlingsflyt.behandling.tilkjentytelse

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.lookup.repository.RepositoryProvider
import javax.sql.DataSource

fun NormalOpenAPIRoute.tilkjentYtelseAPI(dataSource: DataSource) {
    route("/api/behandling") {
        route("/tilkjent/{referanse}") {
            get<BehandlingReferanse, TilkjentYtelseDto> { req ->

                val tilkjentYtelser = dataSource.transaction { connection ->
                    val repositoryFactory = RepositoryProvider(connection)
                    val behandlingRepository = repositoryFactory.provide<BehandlingRepository>()
                    val tilkjentYtelseRepository =
                        repositoryFactory.provide<TilkjentYtelseRepository>()

                    TilkjentYtelseService(
                        behandlingRepository,
                        tilkjentYtelseRepository
                    ).hentTilkjentYtelse(req)
                        .map { TilkjentYtelsePeriode(it.periode, it.tilkjent) }
                }
                respond(TilkjentYtelseDto(perioder = tilkjentYtelser))
            }
        }
    }
}