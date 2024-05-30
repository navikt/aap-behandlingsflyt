package no.nav.aap.behandlingsflyt.drift

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import no.nav.aap.behandlingsflyt.dbconnect.transaction
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.behandlingsflyt.server.respondWithStatus
import no.nav.aap.motor.retry.DriftOppgaverRepositoryExposed
import javax.sql.DataSource


fun NormalOpenAPIRoute.driftApi(dataSource: DataSource) {
    route("/drift/api") {
        route("/rekjor/{referanse}") {
            get<BehandlingReferanse, String> {
                val antallSchedulert = dataSource.transaction { connection ->
                    val behandling = BehandlingReferanseService(connection).behandling(it)
                    DriftOppgaverRepositoryExposed(connection).markerFeilendeForKlar(behandling.id)
                }
                respondWithStatus(
                    HttpStatusCode.OK,
                    "Rekjøring av feilede startet, startet " + antallSchedulert + " oppgaver."
                )
            }
        }
        route("/rekjorAlleFeilede") {
            get<Unit, String> {
                val antallSchedulert = dataSource.transaction { connection ->
                    DriftOppgaverRepositoryExposed(connection).markerAlleFeiledeForKlare()
                }
                respondWithStatus(
                    HttpStatusCode.OK,
                    "Rekjøring av feilede startet, startet " + antallSchedulert + " oppgaver."
                )
            }
        }
    }
}