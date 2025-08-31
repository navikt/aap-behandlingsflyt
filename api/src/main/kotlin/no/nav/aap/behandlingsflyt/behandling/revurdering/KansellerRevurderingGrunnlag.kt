package no.nav.aap.behandlingsflyt.behandling.revurdering

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.kansellerrevurdering.KansellerRevurderingRepository
import no.nav.aap.behandlingsflyt.behandling.kansellerrevurdering.tilDto
import no.nav.aap.behandlingsflyt.behandling.kansellerrevurdering.flate.KansellerRevurderingVurderingDto
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.authorizedGet
import javax.sql.DataSource

data class KansellertRevurderingGrunnlagDto(
    val vurdering: KansellerRevurderingVurderingDto?
)

fun NormalOpenAPIRoute.kansellertRevurderingGrunnlagAPI(dataSource: DataSource, repositoryRegistry: RepositoryRegistry) {
    route("/api/behandling/{referanse}/grunnlag/kansellert-revurdering").authorizedGet<BehandlingReferanse, KansellertRevurderingGrunnlagDto>(
        AuthorizationParamPathConfig(
            operasjon = Operasjon.SE,
            behandlingPathParam = BehandlingPathParam("referanse")
        )
    ) { req ->
        val kansellertRevurderingGrunnlagDto = dataSource.transaction(readOnly = true) { connection ->
            val repositoryProvider = repositoryRegistry.provider(connection)
            val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
            val kansellerRevurderingRepository = repositoryProvider.provide<KansellerRevurderingRepository>()

            val behandlingId = behandlingRepository.hent(BehandlingReferanse(req.referanse)).id

            kansellerRevurderingRepository.hentHvisEksisterer(behandlingId)?.vurdering?.let {
                KansellertRevurderingGrunnlagDto(vurdering = it.tilDto())
            } ?: KansellertRevurderingGrunnlagDto(vurdering = null)
        }
        respond(kansellertRevurderingGrunnlagDto)
    }
}
