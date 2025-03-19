package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.flate

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandRepository
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.authorizedGet
import javax.sql.DataSource

fun NormalOpenAPIRoute.bistandsgrunnlagApi(dataSource: DataSource) {
    route("/api/behandling") {
        route("/{referanse}/grunnlag/bistand") {
            authorizedGet<BehandlingReferanse, BistandGrunnlagDto>(
                AuthorizationParamPathConfig(
                    behandlingPathParam = BehandlingPathParam(
                        "referanse"
                    )
                )
            ) { req ->
                val respons = dataSource.transaction(readOnly = true) { connection ->
                    val repositoryProvider = RepositoryProvider(connection)
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                    val bistandRepository = repositoryProvider.provide<BistandRepository>()
                    val behandling: Behandling =
                        BehandlingReferanseService(behandlingRepository).behandling(req)
                    val historiskeVurderinger =
                        bistandRepository.hentHistoriskeBistandsvurderinger(behandling.sakId, behandling.id)
                    val grunnlag = bistandRepository.hentHvisEksisterer(behandling.id)
                    val vurderingDto = BistandVurderingDto.fraBistandVurdering(grunnlag?.vurdering)
                    BistandGrunnlagDto(
                        vurderingDto,
                        listOfNotNull(vurderingDto),
                        historiskeVurderinger.map { it.toDto() })
                }

                respond(respons)
            }
        }
    }
}
