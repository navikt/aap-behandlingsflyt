package no.nav.aap.behandlingsflyt.behandling.klage.påklagetbehandling

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.ansattinfo.AnsattInfoService
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.påklagetbehandling.PåklagetBehandlingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.påklagetbehandling.PåklagetBehandlingVurderingMedReferanse
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.påklagetbehandling.PåklagetBehandlingVurderingService
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingMedVedtak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.tilgang.kanSaksbehandle
import no.nav.aap.behandlingsflyt.tilgang.relevanteIdenterForBehandlingResolver
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.getGrunnlag
import javax.sql.DataSource

fun NormalOpenAPIRoute.påklagetBehandlingGrunnlagApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider,
) {
    val ansattInfoService = AnsattInfoService(gatewayProvider)
    route("api/klage/{referanse}/grunnlag/påklaget-behandling") {
        getGrunnlag<BehandlingReferanse, PåklagetBehandlingGrunnlagDto>(
            relevanteIdenterResolver = relevanteIdenterForBehandlingResolver(repositoryRegistry, dataSource),
            behandlingPathParam = BehandlingPathParam("referanse"),
            avklaringsbehovKode = Definisjon.FASTSETT_PÅKLAGET_BEHANDLING.kode.toString()
        ) { behandlingReferanse ->
            val respons = dataSource.transaction(readOnly = true) { connection ->
                val repositoryProvider = repositoryRegistry.provider(connection)
                val sakRepository = repositoryProvider.provide<SakRepository>()
                val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                val påklagetBehandlingRepository = repositoryProvider.provide<PåklagetBehandlingRepository>()
                val behandling = behandlingRepository.hent(behandlingReferanse)
                val sak = sakRepository.hent(behandling.sakId)

                val påklagetBehandlingService =
                    PåklagetBehandlingVurderingService(behandlingRepository, påklagetBehandlingRepository)

                val gjeldendeVurdering =
                    påklagetBehandlingService.hentGjeldendeVurderingMedReferanse(behandlingReferanse)
                val behandlingerMedVedtak = påklagetBehandlingService.hentAlleBehandlingerMedVedtakForPerson(sak.person)

                mapTilPåklagetBehandlingGrunnlagDto(
                    gjeldendeVurdering,
                    behandlingerMedVedtak,
                    kanSaksbehandle(),
                    ansattInfoService
                )
            }

            respond(respons)
        }
    }
}

fun mapTilPåklagetBehandlingGrunnlagDto(
    påklagetBehandlingVurderingMedReferanse: PåklagetBehandlingVurderingMedReferanse?,
    behandlingerMedVedtak: List<BehandlingMedVedtak>,
    harTilgangTilÅSaksbehandle: Boolean,
    ansattInfoService: AnsattInfoService
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
        vurdertAv = påklagetBehandlingVurderingMedReferanse?.let {
            VurdertAvResponse.fraIdent(
                it.vurdertAv,
                it.opprettet,
                ansattInfoService
            )
        }
    )
}
        
            