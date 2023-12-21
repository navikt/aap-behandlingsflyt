package no.nav.aap.behandlingsflyt.faktagrunnlag.medlemskap

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.zaxxer.hikari.HikariDataSource
import no.nav.aap.behandlingsflyt.behandling.Behandling
import no.nav.aap.behandlingsflyt.behandling.flate.BehandlingReferanse
import no.nav.aap.behandlingsflyt.dbconnect.transaction
import no.nav.aap.behandlingsflyt.behandling.flate.BehandlingReferanseService

fun NormalOpenAPIRoute.medlemskapsgrunnlagApi(dataSource: HikariDataSource) {
    route("/api/behandling") {
        route("/{referanse}/grunnlag/medlemskap") {
            get<BehandlingReferanse, MedlemskapGrunnlagDto> { req ->
                val behandling: Behandling = dataSource.transaction {
                    BehandlingReferanseService(it).behandling(req)
                }
                respond(MedlemskapGrunnlagDto())
            }
        }
    }
}