package no.nav.aap.pip

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.dbconnect.transaction
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.SakRepositoryImpl
import javax.sql.DataSource

fun NormalOpenAPIRoute.behandlingsflytPip(dataSource: DataSource) {
    route("/pip/api") {
        route("/sak/{saksnummer}/identer") {
            @Suppress("UnauthorizedGet")
            get<SakDTO, IdenterDTO> { req ->
                val saksnummer = req.saksnummer
                val (søker, barn) = dataSource.transaction(readOnly = true) { connection ->
                    val søker = SakRepositoryImpl(connection).finnSøker(Saksnummer(saksnummer))
                    val barn = SakRepositoryImpl(connection).finnBarn(Saksnummer(saksnummer))
                    søker to barn
                }
                respond(
                    IdenterDTO(
                        søker = søker.identer().map { it.identifikator },
                        barn = barn.map { it.identifikator })
                )
            }
        }
    }
    route("/pip/api") {
        route("/behandling/{behandlingsnummer}/identer") {
            @Suppress("UnauthorizedGet")
            get<BehandlingDTO, IdenterDTO> { req ->
                val behandlingsnummer = req.behandlingsnummer
                val (søker, barn) = dataSource.transaction(readOnly = true) { connection ->
                    val søker = BehandlingRepositoryImpl(connection).finnSøker(BehandlingReferanse(behandlingsnummer))
                    val barn = BehandlingRepositoryImpl(connection).finnBarn(BehandlingReferanse(behandlingsnummer))
                    søker to barn
                }
                respond(
                    IdenterDTO(
                        søker = søker.identer().map { it.identifikator },
                        barn = barn.map { it.identifikator })
                )
            }
        }
    }
}

