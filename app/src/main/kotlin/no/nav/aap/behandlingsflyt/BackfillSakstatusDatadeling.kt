package no.nav.aap.behandlingsflyt

import no.nav.aap.behandlingsflyt.behandling.avbrytrevurdering.AvbrytRevurderingService
import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadService
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.prosessering.datadeling.DatadelingMeldePerioderOgSakStatusJobbUtfører
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.motor.FlytJobbRepositoryImpl
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.Prioritet
import org.slf4j.LoggerFactory
import java.time.Duration
import javax.sql.DataSource

private val log = LoggerFactory.getLogger("no.nav.aap.behandlingsflyt.BackfillSakstatusDatadeling")
private val teamLogs = LoggerFactory.getLogger("team-logs")

/**
 * Engangs-backfill: sender oppdatert sakstatus til api-intern for behandlinger med trukket søknad eller
 * avbrutt revurdering, siden disse sakene kan ha fått lagret en utdatert status hos api-intern fra før
 * SakstatusDatadelingService begynte å ta hensyn til trukket/avbrutt. Nye tilfeller trenger ikke backfill,
 * siden BehandlingHendelseServiceImpl allerede sender oppdatert status ved hver behandlingshendelse.
 */
class BackfillSakstatusDatadeling(
    private val dataSource: DataSource,
    gatewayProvider: GatewayProvider,
) {
    private val unleashGateway = gatewayProvider.provide<UnleashGateway>()

    fun kjør() {
        Thread.ofVirtual()
            .name("backfillSakstatusDatadeling")
            .start {
                var forrigeFraTil: List<Long>? = null
                while (true) {
                    if (isLeader(log) && unleashGateway.isEnabled(BehandlingsflytFeature.BackfillSakstatusDatadeling)) {
                        val fraTil = unleashGateway.getVariantValue(
                            BehandlingsflytFeature.BackfillSakstatusDatadeling,
                            "backfill-sak-ider"
                        ).split(",").map(String::toLong)

                        if (forrigeFraTil != fraTil) {
                            try {
                                backfillLoop(fraTil[0], fraTil[1])
                                forrigeFraTil = fraTil /* anser fra/til som forrige kun hvis vi fullfører backfill */
                            } catch (e: Exception) {
                                log.warn(
                                    "BackfillSakstatusDatadeling: uncaughtException {}, se secure / team log",
                                    e.javaClass.name
                                )
                                teamLogs.warn(
                                    "BackfillSakstatusDatadeling: uncaughtException {}: {}",
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

    private fun backfillLoop(fra: Long, til: Long) {
        val sakIder = dataSource.transaction { connection ->
            SakRepositoryImpl(connection).backfillSakstatusDatadelingHentSakIderMellom(fra, til)
        }

        log.info("Begynner backfill av sakstatus-datadeling for ${sakIder.size} saker i sak-id-området $fra – $til")
        var antallOpprettedeJobber = 0

        for (batch in sakIder.chunked(BATCH_STØRRELSE)) {
            for (sakId in batch) {
                dataSource.transaction { connection ->
                    val repositoryProvider = postgresRepositoryRegistry.provider(connection)
                    val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
                    val trukketSøknadService = TrukketSøknadService(repositoryProvider)
                    val avbrytRevurderingService = AvbrytRevurderingService(repositoryProvider)
                    val flytJobbRepository = FlytJobbRepositoryImpl(connection)

                    val behandlingerSomTrengerBackfill = behandlingRepository
                        .hentAlleFor(sakId, TypeBehandling.ytelseBehandlingstyper())
                        .filter {
                            trukketSøknadService.søknadErTrukket(it.id) || avbrytRevurderingService.revurderingErAvbrutt(it.id)
                        }

                    for (behandling in behandlingerSomTrengerBackfill) {
                        flytJobbRepository.leggTil(
                            JobbInput(jobb = DatadelingMeldePerioderOgSakStatusJobbUtfører)
                                .medPayload(behandling.referanse)
                                .forBehandling(sakId.toLong(), behandling.id.toLong())
                                .medPrioritet(Prioritet.BAKGRUNN)
                        )
                        antallOpprettedeJobber += 1
                    }
                }
            }
            log.info("Opprettet {} jobber for backfill av sakstatus-datadeling så langt", antallOpprettedeJobber)
            Thread.sleep(BATCH_PAUSE)
        }
        log.info(
            "Opprettet {} jobber for backfill av sakstatus-datadeling, sak-ider $fra – $til",
            antallOpprettedeJobber
        )
        Thread.sleep(Duration.ofMinutes(5))
    }

    companion object {
        private const val BATCH_STØRRELSE = 100
        private val BATCH_PAUSE = Duration.ofSeconds(30)
    }
}
