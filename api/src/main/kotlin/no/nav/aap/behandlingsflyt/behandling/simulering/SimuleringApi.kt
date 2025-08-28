package no.nav.aap.behandlingsflyt.behandling.simulering

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelseRepository
import no.nav.aap.behandlingsflyt.behandling.utbetaling.UtbetalingGateway
import no.nav.aap.behandlingsflyt.behandling.utbetaling.UtbetalingService
import no.nav.aap.behandlingsflyt.behandling.vedtak.VedtakRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.SamordningAndreStatligeYtelserRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.arbeidsgiver.SamordningArbeidsgiverRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.refusjonkrav.RefusjonkravRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.samordning.refusjonskrav.TjenestepensjonRefusjonsKravVurderingRepository
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.authorizedGet
import no.nav.aap.utbetal.simulering.UtbetalingOgSimuleringDto
import javax.sql.DataSource

fun NormalOpenAPIRoute.simuleringAPI(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider,
) {
    val utbetalingGateway = gatewayProvider.provide(UtbetalingGateway::class)
    route("/api/behandling") {
        route("/{referanse}/utbetaling/simulering") {
            authorizedGet<BehandlingReferanse, List<UtbetalingOgSimuleringDto>>(
                AuthorizationParamPathConfig(
                    operasjon = Operasjon.SE,
                    behandlingPathParam = BehandlingPathParam("referanse")
                )
            ) { req ->
                val tilkjentYtelseDto = dataSource.transaction(readOnly = true) { connection ->
                    val repositoryFactory = repositoryRegistry.provider(connection)
                    val behandlingRepo = repositoryFactory.provide<BehandlingRepository>()
                    val utbetalingService = UtbetalingService(
                        sakRepository = repositoryFactory.provide<SakRepository>(),
                        behandlingRepository = behandlingRepo,
                        tilkjentYtelseRepository = repositoryFactory.provide<TilkjentYtelseRepository>(),
                        avklaringsbehovRepository = repositoryFactory.provide<AvklaringsbehovRepository>(),
                        vedtakRepository = repositoryFactory.provide<VedtakRepository>(),
                        refusjonskravRepository = repositoryFactory.provide<RefusjonkravRepository>(),
                        tjenestepensjonRefusjonsKravVurderingRepository = repositoryFactory.provide<TjenestepensjonRefusjonsKravVurderingRepository>(),
                        samordningAndreStatligeYtelserRepository = repositoryFactory.provide<SamordningAndreStatligeYtelserRepository>(),
                        samordningArbeidsgiverRepository = repositoryFactory.provide<SamordningArbeidsgiverRepository>(),
                        underveisRepository = repositoryFactory.provide<UnderveisRepository>(),
                    )
                    val behandling = behandlingRepo.hent(req)
                    utbetalingService.lagTilkjentYtelseForUtbetaling(behandling.sakId, behandling.id, simulering = true)
                }
                val utbetalingGateway = utbetalingGateway
                if (tilkjentYtelseDto != null) {
                    val simuleringer = utbetalingGateway.simulering(tilkjentYtelseDto)
                    respond(simuleringer)
                } else {
                    respond(emptyList())
                }
            }
        }
    }
}