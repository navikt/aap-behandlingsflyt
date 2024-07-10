package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.flate

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.zaxxer.hikari.HikariDataSource
import no.nav.aap.behandlingsflyt.dbconnect.transaction
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.SoningRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService

fun NormalOpenAPIRoute.soningVurderingAPI(dataSource: HikariDataSource) {
    route("/api/behandling") {
        route("/{referanse}/grunnlag/institusjon/soning") {
            get<BehandlingReferanse, SoningsgrunnlagResponse> { req ->
                val soningsvurdering = dataSource.transaction { connection ->
                    val soningRepository = SoningRepository(connection)
                    val behandling: Behandling = BehandlingReferanseService(connection).behandling(req)
                    soningRepository.hentAktivSoningsvurderingHvisEksisterer(behandling.id)
                }
                respond(SoningsgrunnlagResponse(SoningsvurderingDto.toDto(soningsvurdering)))
            }
        }
    }
}
