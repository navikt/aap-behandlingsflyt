package no.nav.aap.behandlingsflyt

import no.nav.aap.behandlingsflyt.behandling.BackfillBehandlingResultat
import no.nav.aap.behandlingsflyt.behandling.BackfillRedusertDagsatsService
import no.nav.aap.behandlingsflyt.repository.lås.TaSkriveLåsRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature.BackfillRedusertDagsats
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import org.slf4j.LoggerFactory
import java.time.Duration
import javax.sql.DataSource
import kotlin.collections.component1

private val log = LoggerFactory.getLogger("no.nav.aap.behandlingsflyt.BackfillRedusertDagsats")
private val teamLogs = LoggerFactory.getLogger("team-logs")

enum class UnleashVariant(val unleashNavn: String) {
    LISTE("inkluder-sakId-liste"),
    INTERVALL("inkluder-sakId-intervall");
}

class BackfillRedusertDagsats(
    private val dataSource: DataSource,
    private val gatewayProvider: GatewayProvider,
) {
    private val unleashGateway = gatewayProvider.provide<UnleashGateway>()

    /**
     * Backfill kjører kontinuerlig, vær obs ved toggle bruk
     *
     * UnleashVariant-ene vil kun trigge nye runder med backfill hvis saksId-ene
     * endres i input til toggle
     *
     * OBS! Uansett utvalgsmetode i unleash vil app restart/deploy trigge ny kjøring
     * Men kun kandidater som fortsatt mangler redusertDagsats vil backfilles
     */
    fun kjør() {
        Thread.ofVirtual()
            .name("backfillRedusertDagsats")
            .start {
                var forrigeSakIdUtvalg: List<Long>? = null
                while (true) {
                    try {
                        if (erBackfillForutsetningerOppfylt()) {
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

    private fun hentSakIdUtvalg(): List<Long> {
        for (variant in UnleashVariant.entries) {
            if (erUnleashVariantPå(variant.unleashNavn)) {
                return when (variant) {
                    UnleashVariant.LISTE -> hentSakIdUtvalgForVariant(UnleashVariant.LISTE)
                    UnleashVariant.INTERVALL -> hentSakIdUtvalgForVariant(UnleashVariant.INTERVALL)
                }
            }
        }
        return emptyList()
    }

    private fun hentSakIdUtvalgForVariant(variant: UnleashVariant): List<Long> {
        val inkluderSakIder = hentSakIdUtvalgFraUnleashVariant(variant)
        if (inkluderSakIder.any { it == null }) {
            log.warn(
                "Ugyldig verdi i unleash variant '$variant'. Avbryter backfill av redusertDagsats"
            )
            return emptyList()
        }
        return inkluderSakIder.filterNotNull()
    }

    private fun erUnleashVariantPå(variantNavn: String): Boolean =
        unleashGateway.isVariantEnabled(BackfillRedusertDagsats, variantNavn)

    private fun hentSakIdUtvalgFraUnleashVariant(variant: UnleashVariant): List<Long?> {
        val utvalg = unleashGateway.getVariantValue(
            BackfillRedusertDagsats,
            variant.unleashNavn
        )
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map(SakId::fromStringOrNull)
            .map { it?.toLong() }
            .toList()
        when(variant) {
            UnleashVariant.LISTE -> return utvalg
            UnleashVariant.INTERVALL -> {
                if (!erGyldigIntervall(utvalg)) return listOf(null)
                val (første, siste) = utvalg.filterNotNull()
                var utvalgFraIntervall = buildList {
                    for (id in første..siste) {
                        add(id)
                    }
                }
                return utvalgFraIntervall
            }
        }
    }

    private fun erGyldigIntervall(intervallUtvalg: List<Long?>): Boolean {
        if (intervallUtvalg.size != 2) {
            return false
        }
        if (intervallUtvalg.any { it == null }) {
            return false
        }
        val (første, siste) = intervallUtvalg.filterNotNull()
        if (første >= siste) {
            return false
        }
        return true
    }

    /**
     * Backfill rutiner skal kun kjøre på leader pod instansen i gcp og kun når unleash toggle er på
     */
    private fun erBackfillForutsetningerOppfylt(): Boolean {
        return isLeader(log) && unleashGateway.isEnabled(BackfillRedusertDagsats)
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
            // Tillater å avbryte backfill mellom sakIder
            if (!unleashGateway.isEnabled(BackfillRedusertDagsats)) break
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

            val behandlingService = BehandlingService(postgresRepositoryRegistry.provider(connection), gatewayProvider)
            val behandlinger = behandlingService.finnBehandlingerSomManglerRedusertDagsats(sak.id)

            val taSkriveLåsRepository = TaSkriveLåsRepositoryImpl(connection)
            val backfillService = BackfillRedusertDagsatsService(postgresRepositoryRegistry.provider(connection))

            try {
                log.info("Backfiller redusertDagsats for sak ${sak.id} - ${sak.saksnummer}")
                var sakenErFerdigBackfilled = false
                for (behandlingId in behandlinger) {
                    if (sakenErFerdigBackfilled) break
                    taSkriveLåsRepository.withLåstBehandling(behandlingId) {
                        val resultat = backfillService.backfillBehandling(behandlingId)
                        when (resultat) {
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
