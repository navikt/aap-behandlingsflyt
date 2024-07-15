package no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.flate

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelseRepository
import no.nav.aap.behandlingsflyt.dbconnect.transaction
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import javax.sql.DataSource

fun NormalOpenAPIRoute.tilkjentYtelseAPI(dataSource: DataSource) {
    route("/api/behandling") {
        route("/tilkjent/{referanse}") {
            get<BehandlingReferanse, TilkjentYtelseDto> { req ->

                val tilkjentYtelseDto = dataSource.transaction { connection ->
                    val behandling: Behandling =
                        BehandlingReferanseService(BehandlingRepositoryImpl(connection)).behandling(req)
                    val tilkjentYtelse = TilkjentYtelseRepository(connection).hentHvisEksiterer(behandling.id)

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