package no.nav.aap.behandlingsflyt.behandling.klage.påklagetbehandling

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.ansattinfo.AnsattInfoService
import no.nav.aap.behandlingsflyt.behandling.vedtak.VedtakService
import no.nav.aap.behandlingsflyt.behandling.tilbakekrevingsbehandling.TilbakekrevingRepository
import no.nav.aap.behandlingsflyt.behandling.tilbakekrevingsbehandling.Tilbakekrevingsbehandling
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
import org.slf4j.LoggerFactory
import javax.sql.DataSource
import kotlin.collections.map

private val log = LoggerFactory.getLogger("PåklagetBehandlingGrunnlagApi")

fun NormalOpenAPIRoute.påklagetBehandlingGrunnlagApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider,
) {
    val ansattInfoService = AnsattInfoService(gatewayProvider)

    fun hentPåklagetBehandlingGrunnlagDto(
        behandlingReferanse: BehandlingReferanse,
        kanSaksbehandle: Boolean,
    ): PåklagetBehandlingGrunnlagDto {
        return dataSource.transaction(readOnly = true) { connection ->
            val repositoryProvider = repositoryRegistry.provider(connection)
            val sakRepository = repositoryProvider.provide<SakRepository>()
            val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
            val påklagetBehandlingRepository = repositoryProvider.provide<PåklagetBehandlingRepository>()
            val tilbakekrevingRepository = repositoryProvider.provide<TilbakekrevingRepository>()
            val vedtakService = VedtakService(repositoryProvider, gatewayProvider)

            val behandling = behandlingRepository.hent(behandlingReferanse)
            val sak = sakRepository.hent(behandling.sakId)

            val påklagetBehandlingService = PåklagetBehandlingVurderingService(
                behandlingRepository = behandlingRepository,
                påklagetBehandlingRepository = påklagetBehandlingRepository,
                vedtakService = vedtakService,
                tilbakekrevingRepository = tilbakekrevingRepository
            )

            val gjeldendeVurdering =
                påklagetBehandlingService.hentGjeldendeVurderingMedReferanse(behandlingReferanse)

            val behandlingerMedVedtak =
                påklagetBehandlingService.hentAlleBehandlingerMedVedtakForPerson(sak.id)
                    .filterNot {
                        it.vedtakstidspunkt.toLocalDate().isAfter(behandling.opprettetTidspunkt.toLocalDate())
                    }
            val vedtatteKlagebehandlinger = påklagetBehandlingService.hentAlleKlagerMedVedaksdato(sak.id)
                .filterNot { it.vedtaksdato.isAfter(behandling.opprettetTidspunkt.toLocalDate()) }
            val tilbakekrevingsbehandlinger = påklagetBehandlingService.hentAlleAvsluttaTilbakekrevingsbehandlingerForPerson(sak.person.id)

            mapTilPåklagetBehandlingGrunnlagDto(
                påklagetBehandlingVurderingMedReferanse = gjeldendeVurdering,
                behandlingerMedVedtak = behandlingerMedVedtak,
                harTilgangTilÅSaksbehandle = kanSaksbehandle,
                vedtatteKlagebehandlinger,
                tilbakekrevingsbehandlinger,
                ansattInfoService = ansattInfoService,
                sak
            )
        }
    }

    route("api/klage/{referanse}/grunnlag/påklaget-behandling") {
        getGrunnlag<BehandlingReferanse, PåklagetBehandlingGrunnlagDto>(
            relevanteIdenterResolver = relevanteIdenterForBehandlingResolver(repositoryRegistry, dataSource),
            behandlingPathParam = BehandlingPathParam("referanse"),
            påkrevdRolle = Definisjon.FASTSETT_PÅKLAGET_BEHANDLING.løsesAv
        ) { behandlingReferanse: BehandlingReferanse ->
            val grunnlagDto = hentPåklagetBehandlingGrunnlagDto(behandlingReferanse, kanSaksbehandle())
            respond(grunnlagDto)
        }
    }
}

fun mapTilPåklagetBehandlingGrunnlagDto(
    påklagetBehandlingVurderingMedReferanse: PåklagetBehandlingVurderingMedReferanse?,
    behandlingerMedVedtak: List<BehandlingMedVedtak>,
    harTilgangTilÅSaksbehandle: Boolean,
    vedtatteKlagebehandlinger: List<KlagebehandlingMedVedtaksdato>,
    tilbakekrevingsbehandlinger: List<Tilbakekrevingsbehandling>,
    ansattInfoService: AnsattInfoService,
    sak: Sak
): PåklagetBehandlingGrunnlagDto {
    val respons = PåklagetBehandlingGrunnlagDto(
        behandlinger = behandlingerMedVedtak
            .map { BehandlingMedVedtakDto.fraDomene(it) }
            .sortedByDescending { it.vedtakstidspunkt },
        vedtatteKlagebehandlinger = vedtatteKlagebehandlinger.map { KlagebehandlingDto.fraDomene(it, sak.saksnummer) }
            .sortedByDescending { it.vedtaksdato },
        tilbakekrevingsbehandlinger = tilbakekrevingsbehandlinger.map { TilbakekrevingsbehandlingDto.fraDomene(it) }
            .sortedByDescending { it.vedtaksdato?.atStartOfDay() ?: it.opprettetTidspunkt },
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
    return respons
}
        
            