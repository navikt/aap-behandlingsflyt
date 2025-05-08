package no.nav.aap.behandlingsflyt.behandling.simulering

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelseRepository
import no.nav.aap.behandlingsflyt.behandling.utbetaling.UtbetalingGateway
import no.nav.aap.behandlingsflyt.behandling.utbetaling.UtbetalingService
import no.nav.aap.behandlingsflyt.behandling.vedtak.VedtakRepository
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryRegistry
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.authorizedGet
import no.nav.aap.utbetal.simulering.UtbetalingOgSimuleringDto
import javax.sql.DataSource

fun NormalOpenAPIRoute.simuleringAPI(dataSource: DataSource) {
    route("/api/behandling") {
        route("/simulering/{referanse}") {
            authorizedGet<BehandlingReferanse, List<UtbetalingOgSimuleringDto>>(
                AuthorizationParamPathConfig(
                    operasjon = Operasjon.SE,
                    behandlingPathParam = BehandlingPathParam("referanse")
                )
            ) { req ->
                val tilkjentYtelseDto = dataSource.transaction(readOnly = true) { connection ->
                    val repositoryFactory = RepositoryRegistry.provider(connection)
                    val behandlingRepo = repositoryFactory.provide<BehandlingRepository>()
                    val utbetalingService = UtbetalingService(
                        sakRepository = repositoryFactory.provide<SakRepository>(),
                        behandlingRepository = behandlingRepo,
                        tilkjentYtelseRepository = repositoryFactory.provide<TilkjentYtelseRepository>(),
                        avklaringsbehovRepository = repositoryFactory.provide<AvklaringsbehovRepository>(),
                        vedtakRepository = repositoryFactory.provide<VedtakRepository>()
                    )
                    val behandling = behandlingRepo.hent(req)
                    utbetalingService.lagTilkjentYtelseForUtbetaling(behandling.sakId, behandling.id)
                }
                val utbetalingGateway = GatewayProvider.provide(UtbetalingGateway::class)
                if (tilkjentYtelseDto != null) {
                    val simuleringer = utbetalingGateway.simulering(tilkjentYtelseDto)
                    respond(simuleringer)
                } else {
                    respond(listOf())
                }
            }
        }
    }
}