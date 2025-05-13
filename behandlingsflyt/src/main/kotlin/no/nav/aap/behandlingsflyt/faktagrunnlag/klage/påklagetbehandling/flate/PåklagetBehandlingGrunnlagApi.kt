package no.nav.aap.behandlingsflyt.faktagrunnlag.klage.påklagetbehandling.flate

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.påklagetbehandling.PåklagetBehandlingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.påklagetbehandling.PåklagetBehandlingVurderingMedReferanse
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.påklagetbehandling.PåklagetBehandlingVurderingService
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingMedVedtak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.authorizedGet
import javax.sql.DataSource

fun NormalOpenAPIRoute.påklagetBehandlingGrunnlagApi(dataSource: DataSource, repositoryRegistry: RepositoryRegistry) {
    route("api/klage/{referanse}/grunnlag/påklaget-behandling") {
        authorizedGet<BehandlingReferanse, PåklagetBehandlingGrunnlagDto>(
            AuthorizationParamPathConfig(
                behandlingPathParam = BehandlingPathParam(
                    "referanse"
                )
            )
        ) { behandlingReferanse ->
            val respons = dataSource.transaction(readOnly = true) { connection ->
                val repositoryProvider = repositoryRegistry.provider(connection)
                val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                val påklagetBehandlingRepository = repositoryProvider.provide<PåklagetBehandlingRepository>()
                val påklagetBehandlingService = PåklagetBehandlingVurderingService(behandlingRepository, påklagetBehandlingRepository)

                val gjeldendeVurdering = påklagetBehandlingService.hentGjeldendeVurderingMedReferanse(behandlingReferanse)
                val behandlingerMedVedtak = påklagetBehandlingService.hentAlleBehandlingerMedVedtakForSak(behandlingReferanse)

                mapTilPåklagetBehandlingGrunnlagDto(gjeldendeVurdering, behandlingerMedVedtak)
            }

            respond(respons)
        }
    }
}

fun mapTilPåklagetBehandlingGrunnlagDto(påklagetBehandlingVurderingMedReferanse: PåklagetBehandlingVurderingMedReferanse?, behandlingerMedVedtak: List<BehandlingMedVedtak>): PåklagetBehandlingGrunnlagDto {
    return PåklagetBehandlingGrunnlagDto(
        behandlinger = behandlingerMedVedtak
            .map { it.tilBehandlingMedVedtakDto() }
            .sortedByDescending { it.vedtakstidspunkt },
        gjeldendeVurdering = påklagetBehandlingVurderingMedReferanse?.let {
            PåklagetBehandlingVurderingDto(
                påklagetBehandling = påklagetBehandlingVurderingMedReferanse.referanse?.referanse,
                påklagetVedtakType = påklagetBehandlingVurderingMedReferanse.påklagetVedtakType
            )
        }
    )
}
        
            