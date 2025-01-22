package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.flate

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandRepository
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.lookup.repository.RepositoryProvider
import javax.sql.DataSource

fun NormalOpenAPIRoute.bistandsgrunnlagApi(dataSource: DataSource) {
    route("/api/behandling") {
        route("/{referanse}/grunnlag/bistand") {
            get<BehandlingReferanse, BistandGrunnlagDto> { req ->
                val bistandsGrunnlag = dataSource.transaction { connection ->
                    val repositoryProvider = RepositoryProvider(connection)
                    val behandlingRepository = repositoryProvider.provide(BehandlingRepository::class)
                    val bistandRepository = repositoryProvider.provide(BistandRepository::class)
                    val behandling: Behandling =
                        BehandlingReferanseService(behandlingRepository).behandling(req)
                    bistandRepository.hentHvisEksisterer(behandling.id)
                }
                val bistandVurderingDto = BistandVurderingDto.fraBistandVurdering(bistandsGrunnlag?.vurdering)

                respond(BistandGrunnlagDto(bistandVurderingDto))
            }
        }
    }
}
