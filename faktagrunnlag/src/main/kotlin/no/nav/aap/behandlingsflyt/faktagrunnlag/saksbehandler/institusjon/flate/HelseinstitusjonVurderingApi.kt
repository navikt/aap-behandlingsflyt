package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.flate

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.zaxxer.hikari.HikariDataSource
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.HelseinstitusjonService
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.komponenter.dbconnect.transaction

fun NormalOpenAPIRoute.helseinstitusjonVurderingAPI(dataSource: HikariDataSource) {
    route("/api/behandling") {
        route("/{referanse}/grunnlag/institusjon/helse") {
            get<BehandlingReferanse, HelseinstitusjonGrunnlagResponse> { req ->
                val helseinstitusjonGrunnlag = dataSource.transaction { connection ->
                    HelseinstitusjonService(connection).samleHelseinstitusjonGrunnlag(req)
                }
                respond(helseinstitusjonGrunnlag)
            }
        }
    }
}