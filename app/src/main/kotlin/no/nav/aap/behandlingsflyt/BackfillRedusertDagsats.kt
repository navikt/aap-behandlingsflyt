package no.nav.aap.behandlingsflyt

import no.nav.aap.behandlingsflyt.behandling.BackfillBehandlingResultat
import no.nav.aap.behandlingsflyt.behandling.BackfillRedusertDagsatsService
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

private val log = LoggerFactory.getLogger("no.nav.aap.behandlingsflyt.BackfillRedusertDagsats")
private val teamLogs = LoggerFactory.getLogger("team-logs")

class BackfillRedusertDagsats(
    private val dataSource: DataSource,
    private val gatewayProvider: GatewayProvider,
) {
    private val unleashGateway = gatewayProvider.provide<UnleashGateway>()

    fun kjør() {
        Thread.ofVirtual()
            .name("backfillRedusertDagsats")
            .start {
                var forrigeSakIdUtvalg: List<Long>? = null
                while (true) {
                    try {
                        if (erForutsetningerOppfylt()) {
                            val sakIdUtvalg = hentSakIdUtvalg()
                            if (erSakIdUtvalgEndret(forrigeSakIdUtvalg, sakIdUtvalg)) {
                                backfillRedusertDagsatsLoop(sakIdUtvalg)
                                forrigeSakIdUtvalg = sakIdUtvalg
                            }
                        }
                    } catch (e: Exception) {
                        log.warn(
                            "BackfillRedusertDagsats: uncaughtException {}, se secure / team log",
                            e.javaClass.name
                        )
                        teamLogs.warn(
                            "BackfillRedusertDagsats: uncaughtException {}: {}",
                            e.javaClass.name,
                            e.message,
                            e
                        )
                    }
                    Thread.sleep(Duration.ofMinutes(5))
                }
            }
    }

    private fun hentSakIdUtvalg(): List<Long> = unleashGateway.getVariantValue(
        BehandlingsflytFeature.BackfillRedusertDagsats,
        "backfill-saker-ider"
    ).split(",").map(String::toLong)

    /**
     * Backfill rutiner skal kun kjøre på leader pod instansen i gcp og kun når unleash toggle er på
     */
    private fun erForutsetningerOppfylt(): Boolean {
        return isLeader(log) && unleashGateway.isEnabled(BehandlingsflytFeature.BackfillRedusertDagsats)
    }

    private fun erSakIdUtvalgEndret(
        forrigeSakIdUtvalg: List<Long>?,
        sakIder: List<Long>
    ): Boolean = forrigeSakIdUtvalg == null || forrigeSakIdUtvalg != sakIder

    private var antallBackfillUtført = 0

    private fun backfillRedusertDagsatsLoop(sakIder: List<Long>) {
        log.info("Begynner backfill redusertDagsats for sak-ider $sakIder")
        antallBackfillUtført = 0

        for (sakId in sakIder) {
            backfillSak(sakId)
        }

        log.info(
            "Backfill redusertDagsats ferdig: {} behandlinger for sak-ider $sakIder",
            antallBackfillUtført
        )
        Thread.sleep(Duration.ofMinutes(5))
    }

    internal fun backfillSak(sakId: Long) {
        dataSource.transaction { connection ->
            val sakRepository = SakRepositoryImpl(connection)
            val sak = sakRepository.hentSakHvisEksisterer(sakId) ?: return@transaction

            val behandlingService =
                BehandlingService(postgresRepositoryRegistry.provider(connection), gatewayProvider)
            val behandlinger = behandlingService.alleYtelsesbehandlinger(sak.id)

            val taSkriveLåsRepository = TaSkriveLåsRepositoryImpl(connection)
            val backfillService = BackfillRedusertDagsatsService(postgresRepositoryRegistry.provider(connection))

            try {
                log.info("Backfiller redusertDagsats for sak ${sak.id} - ${sak.saksnummer}")
                var sakenErFerdigBackfilled = false
                for (behandling in behandlinger) {
                    if (sakenErFerdigBackfilled) break
                    taSkriveLåsRepository.withLåstBehandling(behandling.id) {
                        val resultat = backfillService.backfillBehandling(behandling)
                        when (resultat) {
                            BackfillBehandlingResultat.AlleredeBackfilled -> {
                                log.info(
                                    "Behandling ${behandling.id.toLong()} i sak ${sak.id.toLong()} " +
                                            "hadde allerede redusertDagsats – stopper backfill for saken"
                                )
                                sakenErFerdigBackfilled = true
                            }

                            BackfillBehandlingResultat.Backfilled -> {
                                antallBackfillUtført++
                                if (antallBackfillUtført % 1000 == 0) {
                                    log.info("Backfilled redusertDagsats for {} behandlinger", antallBackfillUtført)
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                throw RuntimeException(
                    "BackfillRedusertDagsats feilet for sak id=${sak.id.toLong()} nummer=${sak.saksnummer}",
                    e
                )
            }
        }
    }
}
