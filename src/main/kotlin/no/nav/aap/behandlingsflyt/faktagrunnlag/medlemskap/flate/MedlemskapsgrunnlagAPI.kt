package no.nav.aap.behandlingsflyt.faktagrunnlag.medlemskap.flate

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.zaxxer.hikari.HikariDataSource
import no.nav.aap.behandlingsflyt.behandling.Behandling
import no.nav.aap.behandlingsflyt.dbstuff.transaction
import no.nav.aap.behandlingsflyt.faktagrunnlag.BehandlingReferanseService
import no.nav.aap.behandlingsflyt.flate.behandling.BehandlingReferanse

fun NormalOpenAPIRoute.medlemskapsgrunnlagApi(dataSource: HikariDataSource) {
    route("/api/behandling") {
        route("/{referanse}/grunnlag/medlemskap") {
            get<BehandlingReferanse, MedlemskapGrunnlagDto> { req ->
                var behandling: Behandling? = null
                dataSource.transaction {
                    behandling = BehandlingReferanseService(it).behandling(req)
                }
                respond(MedlemskapGrunnlagDto())
            }
        }
    }
}