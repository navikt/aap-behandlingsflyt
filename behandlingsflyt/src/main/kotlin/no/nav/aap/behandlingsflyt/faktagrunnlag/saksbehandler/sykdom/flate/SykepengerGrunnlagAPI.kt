package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.flate

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykepengerErstatningRepository
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.repository.RepositoryProvider
import javax.sql.DataSource


fun NormalOpenAPIRoute.sykepengerGrunnlagApi(dataSource: DataSource) {
    route("/api/behandling") {
        route("/{referanse}/grunnlag/sykdom/sykepengergrunnlag") {
            get<BehandlingReferanse, SykepengerGrunnlagDto> { req ->
                val sykepengerErstatningGrunnlag = dataSource.transaction { connection ->
                    val repositoryProvider = RepositoryProvider(connection)
                    val behandlingRepository = repositoryProvider.provide(BehandlingRepository::class)
                    val behandling: Behandling =
                        BehandlingReferanseService(behandlingRepository).behandling(req)
                    SykepengerErstatningRepository(connection).hentHvisEksisterer(behandling.id)
                }

                respond(SykepengerGrunnlagDto(sykepengerErstatningGrunnlag?.vurdering))
            }
        }
    }
}
