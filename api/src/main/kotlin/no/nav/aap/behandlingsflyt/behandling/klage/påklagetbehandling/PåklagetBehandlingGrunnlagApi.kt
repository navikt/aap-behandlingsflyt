package no.nav.aap.behandlingsflyt.behandling.klage.påklagetbehandling

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.ansattinfo.AnsattInfoService
import no.nav.aap.behandlingsflyt.behandling.vedtak.VedtakService
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurderingerMetaResponse
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.påklagetbehandling.KlagebehandlingMedVedtaksdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.påklagetbehandling.PåklagetBehandlingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.påklagetbehandling.PåklagetBehandlingVurderingMedReferanse
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.påklagetbehandling.PåklagetBehandlingVurderingService
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingMedVedtak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Sak
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
            påkrevdRolle = Definisjon.FASTSETT_PÅKLAGET_BEHANDLING.løsesAv
        ) { behandlingReferanse ->
            val respons = dataSource.transaction(readOnly = true) { connection ->
                val repositoryProvider = repositoryRegistry.provider(connection)
                val sakRepository = repositoryProvider.provide<SakRepository>()
                val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                val påklagetBehandlingRepository = repositoryProvider.provide<PåklagetBehandlingRepository>()
                val vedtakService = VedtakService(repositoryProvider, gatewayProvider)

                val behandling = behandlingRepository.hent(behandlingReferanse)
                val sak = sakRepository.hent(behandling.sakId)

                val påklagetBehandlingService =
                    PåklagetBehandlingVurderingService(
                        behandlingRepository,
                        påklagetBehandlingRepository,
                        vedtakService
                    )

                val gjeldendeVurdering =
                    påklagetBehandlingService.hentGjeldendeVurderingMedReferanse(behandlingReferanse)
                val behandlingerMedVedtak =
                    påklagetBehandlingService.hentAlleBehandlingerMedVedtakForPerson(sak.person.id)
                        .filterNot {
                            it.vedtakstidspunkt.toLocalDate().isAfter(behandling.opprettetTidspunkt.toLocalDate())
                        }


                val vedtatteKlagebehandlinger = påklagetBehandlingService.hentAlleKlagerMedVedaksdato(sak.id)
                    .filterNot { it.vedtaksdato.isAfter(behandling.opprettetTidspunkt.toLocalDate()) }

                mapTilPåklagetBehandlingGrunnlagDto(
                    gjeldendeVurdering,
                    behandlingerMedVedtak,
                    vedtatteKlagebehandlinger,
                    kanSaksbehandle(),
                    ansattInfoService,
                    sak
                )
            }

            respond(respons)
        }
    }
}

fun mapTilPåklagetBehandlingGrunnlagDto(
    påklagetBehandlingVurderingMedReferanse: PåklagetBehandlingVurderingMedReferanse?,
    behandlingerMedVedtak: List<BehandlingMedVedtak>,
    vedtatteKlagebehandlinger: List<KlagebehandlingMedVedtaksdato>,
    harTilgangTilÅSaksbehandle: Boolean,
    ansattInfoService: AnsattInfoService,
    sak: Sak
): PåklagetBehandlingGrunnlagDto {
    return PåklagetBehandlingGrunnlagDto(
        behandlinger = behandlingerMedVedtak
            .map { it.tilBehandlingMedVedtakDto() }
            .sortedByDescending { it.vedtakstidspunkt },
        vedtatteKlagebehandlinger = vedtatteKlagebehandlinger.map { KlagebehandlingDto.fraDomene(it, sak.saksnummer) }
            .sortedByDescending { it.vedtaksdato },
        gjeldendeVurdering = påklagetBehandlingVurderingMedReferanse?.let {
            PåklagetBehandlingVurderingDto(
                påklagetBehandling = påklagetBehandlingVurderingMedReferanse.referanse?.referanse,
                påklagetVedtakType = påklagetBehandlingVurderingMedReferanse.påklagetVedtakType
            )
        },
        harTilgangTilÅSaksbehandle = harTilgangTilÅSaksbehandle,
        vurderingerMeta = VurderingerMetaResponse(
            vurdertAv = påklagetBehandlingVurderingMedReferanse?.let {
                VurdertAvResponse.fraIdent(
                    it.vurdertAv,
                    it.opprettet,
                    ansattInfoService,
                )
            }
        )
    )
}
        
            