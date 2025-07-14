package no.nav.aap.behandlingsflyt.behandling.klage.påklagetbehandling

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.søknad.DatoFraDokumentUtleder
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.påklagetbehandling.PåklagetBehandlingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.påklagetbehandling.PåklagetBehandlingVurderingMedReferanse
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.påklagetbehandling.PåklagetBehandlingVurderingService
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingMedVedtak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.tilgang.TilgangGateway
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.httpklient.auth.token
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
                val sakRepository = repositoryProvider.provide<SakRepository>()
                val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                val påklagetBehandlingRepository = repositoryProvider.provide<PåklagetBehandlingRepository>()
                val mottattDokumentRepository = repositoryProvider.provide<MottattDokumentRepository>()
                val behandling = behandlingRepository.hent(behandlingReferanse)
                val sak = sakRepository.hent(behandling.sakId)

                val påklagetBehandlingService =
                    PåklagetBehandlingVurderingService(behandlingRepository, påklagetBehandlingRepository)

                val gjeldendeVurdering =
                    påklagetBehandlingService.hentGjeldendeVurderingMedReferanse(behandlingReferanse)
                val behandlingerMedVedtak = påklagetBehandlingService.hentAlleBehandlingerMedVedtakForPerson(sak.person)

                val harTilgangTilÅSaksbehandle =
                    GatewayProvider.provide<TilgangGateway>().sjekkTilgangTilBehandling(
                        behandling.referanse.referanse,
                        Definisjon.FASTSETT_PÅKLAGET_BEHANDLING,
                        token()
                    )
                mapTilPåklagetBehandlingGrunnlagDto(
                    gjeldendeVurdering,
                    behandlingerMedVedtak,
                    harTilgangTilÅSaksbehandle
                )
            }

            respond(respons)
        }
    }
}

fun mapTilPåklagetBehandlingGrunnlagDto(
    påklagetBehandlingVurderingMedReferanse: PåklagetBehandlingVurderingMedReferanse?,
    behandlingerMedVedtak: List<BehandlingMedVedtak>,
    harTilgangTilÅSaksbehandle: Boolean
): PåklagetBehandlingGrunnlagDto {
    return PåklagetBehandlingGrunnlagDto(
        behandlinger = behandlingerMedVedtak
            .map { it.tilBehandlingMedVedtakDto() }
            .sortedByDescending { it.vedtakstidspunkt },
        gjeldendeVurdering = påklagetBehandlingVurderingMedReferanse?.let {
            PåklagetBehandlingVurderingDto(
                påklagetBehandling = påklagetBehandlingVurderingMedReferanse.referanse?.referanse,
                påklagetVedtakType = påklagetBehandlingVurderingMedReferanse.påklagetVedtakType
            )
        },
        harTilgangTilÅSaksbehandle = harTilgangTilÅSaksbehandle,
        vurdertAv = påklagetBehandlingVurderingMedReferanse?.let { VurdertAvResponse.fraIdent(it.vurdertAv, it.opprettet) }
    )
}
        
            