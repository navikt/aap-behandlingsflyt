package no.nav.aap.behandlingsflyt

import no.nav.aap.behandlingsflyt.behandling.BackfillRedusertDagsatsService
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingService
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

sealed interface SammenlignbartUtvalg {
    data class Intervall(val start: Long, val stopp: Long) : SammenlignbartUtvalg
    data class TallListe(val tall: List<Long>) : SammenlignbartUtvalg
}

class BackfillRedusertDagsats(
    private val dataSource: DataSource,
    private val gatewayProvider: GatewayProvider,
) {
    private val unleashGateway = gatewayProvider.provide<UnleashGateway>()
    private var antallBackfillUtførtIKjøring = 0

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
                var forrigeUtvalg: SammenlignbartUtvalg? = null
                while (true) {
                    try {
                        if (erBackfillForutsetningerOppfylt()) {
                            val sakIdUtvalg = hentSakIdUtvalg()
                            val aktueltUtvalg = mapTilSammenlignbartUtvalg(sakIdUtvalg)
                            if (erUtvalgEndret(forrigeUtvalg, aktueltUtvalg)) {
                                backfillRedusertDagsatsLoop(sakIdUtvalg)
                                forrigeUtvalg = aktueltUtvalg
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

    private fun mapTilSammenlignbartUtvalg(sakIdUtvalg: Iterable<Long>): SammenlignbartUtvalg {
        val aktueltUtvalg = when (sakIdUtvalg) {
            is LongRange -> SammenlignbartUtvalg.Intervall(sakIdUtvalg.first, sakIdUtvalg.last)
            is List<Long> -> SammenlignbartUtvalg.TallListe(sakIdUtvalg)
            else -> SammenlignbartUtvalg.TallListe(sakIdUtvalg.toList())
        }
        return aktueltUtvalg
    }

    private fun hentSakIdUtvalg(): Iterable<Long> {
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

    private fun hentSakIdUtvalgForVariant(variant: UnleashVariant): Iterable<Long> {
        val inkluderSakIder = hentSakIdUtvalgFraUnleashVariant(variant)
        if (inkluderSakIder is List<Long> && (inkluderSakIder.isEmpty())) {
            log.warn(
                "Ugyldig verdi i unleash variant '$variant'. Avbryter backfill av redusertDagsats"
            )
        }
        return inkluderSakIder
    }

    private fun erUnleashVariantPå(variantNavn: String): Boolean =
        unleashGateway.isVariantEnabled(BackfillRedusertDagsats, variantNavn)

    private fun hentSakIdUtvalgFraUnleashVariant(variant: UnleashVariant): Iterable<Long> {
        val utvalgInput: List<Long?> = unleashGateway.getVariantValue(
            BackfillRedusertDagsats,
            variant.unleashNavn
        )
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { it.toLongOrNull() }
            .toList()
        val utvalg: List<Long> = if (utvalgInput.any { it == null }) emptyList() else utvalgInput.filterNotNull()
        when(variant) {
            UnleashVariant.LISTE -> return utvalg
            UnleashVariant.INTERVALL -> {
                if (utvalg.isEmpty() || !erGyldigIntervall(utvalg)) {
                    return emptySet()
                }
                val (første, siste) = utvalg
                return første..siste
            }
        }
    }

    private fun erGyldigIntervall(intervallUtvalg: List<Long>): Boolean {
        if (intervallUtvalg.size != 2) {
            return false
        }
        val (første, siste) = intervallUtvalg
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

    private fun erUtvalgEndret(
        forrigeUtvalg: SammenlignbartUtvalg?,
        aktueltUtvalg: SammenlignbartUtvalg
    ): Boolean = forrigeUtvalg == null || forrigeUtvalg != aktueltUtvalg


    private fun backfillRedusertDagsatsLoop(sakIder: Iterable<Long>) {
        log.info("Begynner backfill redusertDagsats for sak-ider $sakIder")
        antallBackfillUtførtIKjøring = 0

        for (sakId in sakIder) {
            // Tillater å avbryte backfill mellom sakIder
            if (!unleashGateway.isEnabled(BackfillRedusertDagsats)) break
            val antallBackfillUtførtISak = backfillSak(sakId)
            antallBackfillUtførtIKjøring += antallBackfillUtførtISak
        }

        log.info("Backfill redusertDagsats ferdig: ${antallBackfillUtførtIKjøring} rader backfillet for sak-ider $sakIder")
        Thread.sleep(Duration.ofMinutes(5))
    }

    internal fun backfillSak(sakId: Long) : Int =
        dataSource.transaction { connection ->
            var antallBackfillUtførtISak = 0
            val sakRepository = SakRepositoryImpl(connection)
            val sak = sakRepository.hentSakHvisEksisterer(sakId)
                ?: return@transaction 0

            val behandlingService = BehandlingService(postgresRepositoryRegistry.provider(connection), gatewayProvider)
            val behandlinger = behandlingService.finnBehandlingerSomManglerRedusertDagsats(sak.id)

            val backfillService = BackfillRedusertDagsatsService(postgresRepositoryRegistry.provider(connection))

            try {
                log.info("Backfiller redusertDagsats for sak ${sak.id} - ${sak.saksnummer}")
                var sakenErFerdigBackfilled = false
                for (behandlingId in behandlinger) {
                    if (sakenErFerdigBackfilled) break
                    val antallBackfillUtført = backfillService.backfillBehandling(behandlingId)
                    if (antallBackfillUtført > 0) {
                        antallBackfillUtførtISak += antallBackfillUtført
                        if (antallBackfillUtførtISak % 1000 == 0) {
                            log.info("Backfilled redusertDagsats for {} tilkjent_periode rader", antallBackfillUtført)
                        }
                    }
                }
            } catch (e: Exception) {
                throw RuntimeException(
                    "BackfillRedusertDagsats feilet for sak id=${sak.id.toLong()} nummer=${sak.saksnummer}",
                    e
                )
            }

            antallBackfillUtførtISak
    }
}
