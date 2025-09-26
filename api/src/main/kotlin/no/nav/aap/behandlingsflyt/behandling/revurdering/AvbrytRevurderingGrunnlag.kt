package no.nav.aap.behandlingsflyt.behandling.revurdering

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.avbrytrevurdering.AvbrytRevurderingRepository
import no.nav.aap.behandlingsflyt.behandling.avbrytrevurdering.tilDto
import no.nav.aap.behandlingsflyt.behandling.avbrytrevurdering.flate.AvbrytRevurderingVurderingDto
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.authorizedGet
import javax.sql.DataSource

data class AvbrytRevurderingGrunnlagDto(
    val vurdering: AvbrytRevurderingVurderingDto?
)

fun NormalOpenAPIRoute.avbrytRevurderingGrunnlagAPI(dataSource: DataSource, repositoryRegistry: RepositoryRegistry) {
    route("/api/behandling/{referanse}/grunnlag/avbryt-revurdering").authorizedGet<BehandlingReferanse, AvbrytRevurderingGrunnlagDto>(
        AuthorizationParamPathConfig(
            operasjon = Operasjon.SE,
            behandlingPathParam = BehandlingPathParam("referanse")
        )
    ) { req ->
        val avbrytRevurderingGrunnlagDto = dataSource.transaction(readOnly = true) { connection ->
            val repositoryProvider = repositoryRegistry.provider(connection)
            val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
            val avbrytRevurderingRepository = repositoryProvider.provide<AvbrytRevurderingRepository>()

            val behandlingId = behandlingRepository.hent(BehandlingReferanse(req.referanse)).id

            avbrytRevurderingRepository.hentHvisEksisterer(behandlingId)?.vurdering?.let {
                AvbrytRevurderingGrunnlagDto(vurdering = it.tilDto())
            } ?: AvbrytRevurderingGrunnlagDto(vurdering = null)
        }
        respond(avbrytRevurderingGrunnlagDto)
    }
}
