package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.behandling.gregulering.GReguleringService
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.Grunnbeløp
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature.GReguleringUtplukkJobb
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProvidersJobbSpesifikasjon
import no.nav.aap.motor.cron.CronExpression
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.LocalDate
import java.time.Year

/**
 * Kjøres daglig og oppretter jobber for G-Regulering. Jobber opprettes for saksbehandlingskandidater hvor innvilget
 * AAP-periode ikke har en endring i grunnbeløpet benyttet i beregninger av ytelse for ny G-periode. Dette forutsetter
 * at ny G-justering finnes i Grunnbeløp.kt for inneværende G-periode. G-periode utledes fra jobb-kjøretidspunktet.
 *
 * Alle G-Justeringen fra før Kelvin ble lansert i produksjon i 2025 ignoreres for G-regulering. Første aktuelle
 * G-periode er 2026/2027.
 *
 * G-regulering medfører omberegninger, slik at ytelsen blir korrekt både før og etter G-justering.
 *
 * G-perioden løper fra 1. mai hvert år til 30. april neste år. G-justeringen for år N (datert 1. mai N)
 * er aktuell fra 1. mai N til 30. april N+1.
 */
class OpprettJobbForGReguleringJobbUtfører(
    private val gReguleringService: GReguleringService,
    private val flytJobbRepository: FlytJobbRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val unleashGateway: UnleashGateway,
) : JobbUtfører {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(input: JobbInput) {
        if (unleashGateway.isDisabled(GReguleringUtplukkJobb)) {
            log.info("Feature toggle GReguleringUtplukkJobb er avskrudd, hopper over opprettelse av G-regulerings-jobber")
            return
        }

        val gPeriodeÅr = gPeriodeÅr(LocalDate.now(clock))
        val aktuellGJustering = hentAktuellGJustering(gPeriodeÅr)
        if (aktuellGJustering == null || aktuellGJustering.dato.isBefore(LocalDate.of(2025, 5, 1))) {
            log.info("Avslutter søk etter G-reguleringskandidater. Ingen post 2025 G-justering funnet for G-periode-år: ${gPeriodeÅr} i Gunnbeløp.kt")
            return
        }

        val saker = hentKandidaterForGRegulering(aktuellGJustering.dato)

        log.info("Fant ${saker.size} kandidater for G-regulering for gitt G-justering ${aktuellGJustering?.dato}")
        saker
            .filterNot { finnesAlleredeGReguleringJobbForSak(it) }
            .also { log.info("Oppretter jobber for alle saker som er aktuelle kandidater for G-regulering. Antall = ${it.size}, Saker = $it") }
            .forEach {
                flytJobbRepository.leggTil(JobbInput(OpprettBehandlingGReguleringJobbUtfører).forSak(it.toLong()))
            }
    }

    private fun hentAktuellGJustering(år: Year) : Grunnbeløp.GrunnbeløpMedDato? {
        return gReguleringService.finnesGrunnbeløpForÅr(år)
    }

    internal fun gPeriodeÅr(dato: LocalDate): Year =
        if (dato.monthValue < 5) Year.of(dato.year - 1) else Year.of(dato.year)

    private fun hentKandidaterForGRegulering(datoForGJustering: LocalDate): Set<SakId> {
        val alleSaker = gReguleringService.hentSakerForGRegulering(datoForGJustering)
        log.info("Antall saker som er kandidater for G-regulering: ${alleSaker.size}")

        if (gradvisUtrulling("sak-id-filter")) {
            val sakIdTekster = unleashGateway.getVariantValue(GReguleringUtplukkJobb, "sak-id-filter")
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            val parsedSakIder = sakIdTekster.map { it.toLongOrNull() }
            if (parsedSakIder.any { it == null }) {
                log.warn("Ugyldig verdi i sak-id-filter. Hopper over opprettelse av G-reguleringsjobber")
                return emptySet()
            }
            val sakIdFilter = parsedSakIder.filterNotNull().toSet()
            return alleSaker.filter { it.id in sakIdFilter }.toSet()
        } else if (gradvisUtrulling("maks-antall-saker")) {
            val maksAntallSaker = unleashGateway.getVariantValue(GReguleringUtplukkJobb, "maks-antall-saker")
                .trim()
                .toIntOrNull() ?: return emptySet()
            return alleSaker.take(maksAntallSaker).toSet()
        } else {
            return emptySet()
        }
    }

    private fun finnesAlleredeGReguleringJobbForSak(sakId: SakId): Boolean =
        flytJobbRepository.hentJobberForSak(sakId.toLong())
            .any { it.type() == OpprettBehandlingGReguleringJobbUtfører.type }

    private fun gradvisUtrulling(variantNavn: String): Boolean =
        unleashGateway.isVariantEnabled(GReguleringUtplukkJobb, variantNavn)

    companion object : ProvidersJobbSpesifikasjon {
        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): JobbUtfører {
            return OpprettJobbForGReguleringJobbUtfører(
                gReguleringService = GReguleringService(repositoryProvider, gatewayProvider),
                flytJobbRepository = repositoryProvider.provide(),
                unleashGateway = gatewayProvider.provide(),
            )
        }

        override val type = "batch.OpprettJobbForGRegulering"
        override val navn = "Opprett jobber for G-regulering av saker grunnet årlig G-justering"
        override val beskrivelse = "Skal opprette jobber for omberegning av ytelse i saker, G-regulering"

        /**
         * Kjøres hver dag kl 05:00 og 17:00
         */
        override val cron = CronExpression.createWithoutSeconds("0 5,17 * * *")
    }
}