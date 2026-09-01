package no.nav.aap.behandlingsflyt

import no.nav.aap.behandlingsflyt.behandling.BackfillBehandlingResultat
import no.nav.aap.behandlingsflyt.behandling.BackfillKravService
import no.nav.aap.behandlingsflyt.repository.lås.TaSkriveLåsRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingService
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import org.slf4j.LoggerFactory
import java.time.Duration
import javax.sql.DataSource

private val log = LoggerFactory.getLogger("no.nav.aap.behandlingsflyt.BackfillKrav")
private val teamLogs = LoggerFactory.getLogger("team-logs")

class BackfillKrav(
    private val dataSource: DataSource,
    private val gatewayProvider: GatewayProvider,
) {
    private val unleashGateway = gatewayProvider.provide<UnleashGateway>()

    fun kjør() {
        Thread.ofVirtual()
            .name("backfillKrav")
            .start {
                var forrigeFraTil: List<Long>? = null
                while (true) {
                    if (isLeader(log) && unleashGateway.isEnabled(BehandlingsflytFeature.BackfillKrav)) {
                        val fraTil = unleashGateway.getVariantValue(
                            BehandlingsflytFeature.BackfillKrav,
                            "backfill-saker-ider"
                        ).split(",").map(String::toLong)

                        if (forrigeFraTil != fraTil) {
                            try {
                                backfillKravLoop(fraTil[0], fraTil[1])
                                forrigeFraTil = fraTil
                            } catch (e: Exception) {
                                log.warn("BackfillKrav: uncaughtException {}, se secure / team log", e.javaClass.name)
                                teamLogs.warn(
                                    "BackfillKrav: uncaughtException {}: {}",
                                    e.javaClass.name,
                                    e.message,
                                    e
                                )
                            }
                        }
                    }
                    Thread.sleep(Duration.ofMinutes(5))
                }
            }
    }

    private var antallBackfillUtført = 0

    private fun backfillKravLoop(fra: Long, til: Long) {
        log.info("Begynner backfill krav for sak-ider $fra – $til")
        antallBackfillUtført = 0

        for (sakId in fra..til) {
            dataSource.transaction { connection ->
                val sakRepository = SakRepositoryImpl(connection)
                val sak = sakRepository.hentSakHvisEksisterer(sakId) ?: return@transaction

                val behandlingService =
                    BehandlingService(postgresRepositoryRegistry.provider(connection), gatewayProvider)
                val behandlinger = behandlingService.alleYtelsesbehandlinger(sak.id)

                val taSkriveLåsRepository = TaSkriveLåsRepositoryImpl(connection)
                val backfillService = BackfillKravService(postgresRepositoryRegistry.provider(connection))

                if (backfillService.erTrukketSøknadSak(behandlinger)) {
                    log.info("Hopper over sak ${sak.id.toLong()} – søknad er trukket")
                    return@transaction
                }

                try {
                    var sakenErFerdigBackfilled = false
                    for (behandling in behandlinger) {
                        if (sakenErFerdigBackfilled) break
                        taSkriveLåsRepository.withLåstBehandling(behandling.id) {
                            val resultat = backfillService.backfillBehandling(sak, behandling, erNyesteBehandling = behandling == behandlinger.last())
                            when (resultat) {
                                BackfillBehandlingResultat.AlleredeBackfilled -> {
                                    log.info(
                                        "Behandling ${behandling.id.toLong()} i sak ${sak.id.toLong()} " +
                                            "hadde allerede krav – stopper backfill for saken"
                                    )
                                    sakenErFerdigBackfilled = true
                                }

                                BackfillBehandlingResultat.Backfilled -> {
                                    antallBackfillUtført++
                                    if (antallBackfillUtført % 1000 == 0) {
                                        log.info("Backfilled krav for {} behandlinger", antallBackfillUtført)
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    throw RuntimeException(
                        "BackfillKrav feilet for sak id=${sak.id.toLong()} nummer=${sak.saksnummer}",
                        e
                    )
                }
            }
        }

        log.info(
            "Backfill krav ferdig: {} behandlinger for sak-ider $fra – $til",
            antallBackfillUtført
        )
        Thread.sleep(Duration.ofMinutes(5))
    }
}
