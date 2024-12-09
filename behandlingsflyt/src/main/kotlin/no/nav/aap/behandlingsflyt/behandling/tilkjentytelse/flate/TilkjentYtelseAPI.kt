package no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.flate

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelseRepositoryImpl
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.repository.RepositoryFactory
import javax.sql.DataSource

fun NormalOpenAPIRoute.tilkjentYtelseAPI(dataSource: DataSource) {
    route("/api/behandling") {
        route("/tilkjent/{referanse}") {
            get<BehandlingReferanse, TilkjentYtelseDto> { req ->

                val tilkjentYtelseDto = dataSource.transaction { connection ->
                    val repositoryFactory = RepositoryFactory(connection)
                    val behandlingRepository = repositoryFactory.create(BehandlingRepository::class)
                    val behandling: Behandling =
                        BehandlingReferanseService(behandlingRepository).behandling(req)
                    val tilkjentYtelse = TilkjentYtelseRepositoryImpl(connection).hentHvisEksisterer(behandling.id)

                    if (tilkjentYtelse == null) return@transaction TilkjentYtelseDto(emptyList())

                    val tilkjentYtelsePerioder = tilkjentYtelse.map {
                        TilkjentYtelsePeriode(it.periode, it.verdi)
                    }

                    TilkjentYtelseDto(tilkjentYtelsePerioder)
                }
                respond(tilkjentYtelseDto)
            }
        }
    }
}