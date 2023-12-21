package no.nav.aap.behandlingsflyt.faktagrunnlag.bistand.flate

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.zaxxer.hikari.HikariDataSource
import no.nav.aap.behandlingsflyt.behandling.Behandling
import no.nav.aap.behandlingsflyt.behandling.flate.BehandlingReferanse
import no.nav.aap.behandlingsflyt.dbconnect.transaction
import no.nav.aap.behandlingsflyt.behandling.flate.BehandlingReferanseService
import no.nav.aap.behandlingsflyt.faktagrunnlag.bistand.BistandRepository

fun NormalOpenAPIRoute.bistandsgrunnlagApi(dataSource: HikariDataSource) {
    route("/api/behandling") {
        route("/{referanse}/grunnlag/bistand") {
            get<BehandlingReferanse, BistandGrunnlagDto> { req ->
                val bistandsGrunnlag = dataSource.transaction { connection ->
                    val behandling: Behandling = BehandlingReferanseService(connection).behandling(req)
                    val bistandRepository = BistandRepository(connection)
                    bistandRepository.hentHvisEksisterer(behandling.id)
                }

                respond(BistandGrunnlagDto(bistandsGrunnlag?.vurdering))
            }
        }
    }
}
