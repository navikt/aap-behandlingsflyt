package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.flate

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import com.zaxxer.hikari.HikariDataSource
import no.nav.aap.behandlingsflyt.dbconnect.transaction
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.HelseinstitusjonRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService

fun NormalOpenAPIRoute.helseinstitusjonVurderingAPI(dataSource: HikariDataSource) {
    route("/api/behandling") {
        route("/{referanse}/grunnlag/institusjon/helse") {
            get<BehandlingReferanse, HelseinstitusjonGrunnlagResponse> { req ->
                val helseinstitusjonVurdering = dataSource.transaction { connection ->
                    val helseinstitusjonRepository = HelseinstitusjonRepository(connection)
                    val behandlingRepository = BehandlingRepositoryImpl(connection)
                    val behandling: Behandling = BehandlingReferanseService(behandlingRepository).behandling(req)
                    helseinstitusjonRepository.hentAktivHelseinstitusjonVurderingHvisEksisterer(behandling.id)
                }
                respond(HelseinstitusjonGrunnlagResponse(HelseinstitusjonVurderingDto.toDto(helseinstitusjonVurdering)))
            }
        }
    }
}