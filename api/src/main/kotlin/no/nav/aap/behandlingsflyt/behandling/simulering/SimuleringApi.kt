package no.nav.aap.behandlingsflyt.behandling.simulering

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import no.nav.aap.behandlingsflyt.behandling.utbetaling.UtbetalingGateway
import no.nav.aap.behandlingsflyt.behandling.utbetaling.UtbetalingService
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.tilgang.relevanteIdenterForBehandlingResolver
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.tilgang.AuthorizationParamPathConfig
import no.nav.aap.tilgang.BehandlingPathParam
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.authorizedGet
import no.nav.aap.utbetal.simulering.SimuleringDto
import no.nav.aap.utbetal.simulering.UtbetalingOgSimuleringDto
import javax.sql.DataSource

fun NormalOpenAPIRoute.simuleringApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider,
) {
    val utbetalingGateway = gatewayProvider.provide(UtbetalingGateway::class)
    route("/api/behandling") {
        route("/{referanse}/utbetaling/simulering") {
            authorizedGet<BehandlingReferanse, List<UtbetalingOgSimuleringDto>>(
                AuthorizationParamPathConfig(
                    relevanteIdenterResolver = relevanteIdenterForBehandlingResolver(repositoryRegistry, dataSource),
                    operasjon = Operasjon.SE,
                    behandlingPathParam = BehandlingPathParam("referanse")
                )
            ) { req ->
                val tilkjentYtelseDto = dataSource.transaction(readOnly = true) { connection ->
                    val repositoryFactory = repositoryRegistry.provider(connection)
                    val behandlingRepo = repositoryFactory.provide<BehandlingRepository>()
                    val utbetalingService = UtbetalingService(
                        repositoryProvider = repositoryFactory,
                        gatewayProvider = gatewayProvider
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

        // v2: eksponerer aap-utbetal sitt nye /simulering/v2-endepunkt. Lever i parallell
        // med v1-ruten over inntil alle saker er migrert til nytt utbetalings-api.
        route("/{referanse}/utbetaling/simulering/v2") {
            authorizedGet<BehandlingReferanse, SimuleringDto>(
                AuthorizationParamPathConfig(
                    relevanteIdenterResolver = relevanteIdenterForBehandlingResolver(repositoryRegistry, dataSource),
                    operasjon = Operasjon.SE,
                    behandlingPathParam = BehandlingPathParam("referanse")
                )
            ) { req ->
                val tilkjentYtelseDto = dataSource.transaction(readOnly = true) { connection ->
                    val repositoryFactory = repositoryRegistry.provider(connection)
                    val behandlingRepo = repositoryFactory.provide<BehandlingRepository>()
                    val utbetalingService = UtbetalingService(
                        repositoryProvider = repositoryFactory,
                        gatewayProvider = gatewayProvider
                    )
                    val behandling = behandlingRepo.hent(req)
                    utbetalingService.lagTilkjentYtelseForUtbetaling(behandling.sakId, behandling.id, simulering = true)
                }
                if (tilkjentYtelseDto != null) {
                    val simulering = utbetalingGateway.simuleringV2(tilkjentYtelseDto)
                    respond(simulering)
                } else {
                    respond(SimuleringDto(perioder = emptyList()))
                }
            }
        }
    }
}