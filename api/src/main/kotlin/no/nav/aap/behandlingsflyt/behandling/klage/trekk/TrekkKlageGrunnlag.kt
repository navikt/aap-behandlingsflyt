package no.nav.aap.behandlingsflyt.behandling.klage.trekk
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.trekkklage.TrekkKlageRepository
import no.nav.aap.behandlingsflyt.behandling.trekkklage.flate.TrekkKlageVurderingDto
import no.nav.aap.behandlingsflyt.behandling.trekkklage.tilDto
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.authorizedGet
import javax.sql.DataSource


class TrekkKlageGrunnlagDto(
    val vurdering: TrekkKlageVurderingDto?
)

fun NormalOpenAPIRoute.trekkKlageGrunnlagAPI(dataSource: DataSource, repositoryRegistry: RepositoryRegistry) {
    route("/api/klage/{referanse}/grunnlag/trekk-klage").authorizedGet<BehandlingReferanse, TrekkKlageGrunnlagDto>(
        AuthorizationParamPathConfig(
            operasjon = Operasjon.SE,
            behandlingPathParam = BehandlingPathParam("referanse")
        )
    ) { req ->
        val trekkKlageGrunnlagDto = dataSource.transaction(readOnly = true) { connection ->
            val repositoryProvider = repositoryRegistry.provider(connection)
            val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
            val trekkKlageRepository = repositoryProvider.provide<TrekkKlageRepository>()

            val behandlingId = behandlingRepository.hent(BehandlingReferanse(req.referanse)).id

            trekkKlageRepository.hentTrekkKlageGrunnlag(behandlingId)?.vurdering?.let {
                TrekkKlageGrunnlagDto(vurdering = it.tilDto())
            } ?: TrekkKlageGrunnlagDto(vurdering = null)
        }
        respond(trekkKlageGrunnlagDto)
    }
}
