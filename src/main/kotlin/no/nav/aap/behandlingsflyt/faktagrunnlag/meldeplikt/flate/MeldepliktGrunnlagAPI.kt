package no.nav.aap.behandlingsflyt.faktagrunnlag.meldeplikt.flate

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.zaxxer.hikari.HikariDataSource
import no.nav.aap.behandlingsflyt.faktagrunnlag.BehandlingReferanseService
import no.nav.aap.behandlingsflyt.faktagrunnlag.meldeplikt.MeldepliktRepository
import no.nav.aap.behandlingsflyt.flate.behandling.BehandlingReferanse

fun NormalOpenAPIRoute.meldepliktsgrunnlagApi(dataSource: HikariDataSource) {
    route("/api/behandling") {
        route("/{referanse}/grunnlag/fritak-meldeplikt") {
            get<BehandlingReferanse, FritakMeldepliktGrunnlagDto> { req ->
                val behandling = BehandlingReferanseService.behandling(req)

                val meldepliktGrunnlag = MeldepliktRepository.hentHvisEksisterer(behandling.id)
                respond(FritakMeldepliktGrunnlagDto(meldepliktGrunnlag?.vurderinger.orEmpty()))
            }
        }
    }
}