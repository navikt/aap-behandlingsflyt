package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.flate

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.zaxxer.hikari.HikariDataSource
import no.nav.aap.behandlingsflyt.dbconnect.transaction
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService

fun NormalOpenAPIRoute.meldepliktsgrunnlagApi(dataSource: HikariDataSource) {
    route("/api/behandling") {
        route("/{referanse}/grunnlag/fritak-meldeplikt") {
            get<BehandlingReferanse, FritakMeldepliktGrunnlagDto> { req ->
                val meldepliktGrunnlag = dataSource.transaction { connection ->
                    val behandling: Behandling = BehandlingReferanseService(connection).behandling(req)
                    MeldepliktRepository(connection).hentHvisEksisterer(behandling.id)
                }

                respond(FritakMeldepliktGrunnlagDto(meldepliktGrunnlag?.vurderinger.orEmpty()))
            }
        }
    }
}