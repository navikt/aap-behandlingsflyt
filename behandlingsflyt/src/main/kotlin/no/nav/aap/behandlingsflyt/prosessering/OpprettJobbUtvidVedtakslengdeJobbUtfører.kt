package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.behandling.vedtakslengde.VedtakslengdeUtvidelse
import no.nav.aap.behandlingsflyt.behandling.vedtakslengde.VedtakslengdeService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
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
import java.time.LocalDate.now

/**
 * Forlenger vedtaket på alle saker som har oppfylt ett år med rettighetstype bistandsbehov fra forrige sluttdato.
 * På sikt vil denne jobben også utvide andre vedtak hvor utvidelsen kan automatiseres slik som overgang alder,
 * utløp av kvote osv.
 */
class OpprettJobbUtvidVedtakslengdeJobbUtfører(
    private val behandlingService: BehandlingService,
    private val vedtakslengdeService: VedtakslengdeService,
    private val flytJobbRepository: FlytJobbRepository,
    private val clock: Clock = Clock.systemDefaultZone()
) : JobbUtfører {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(input: JobbInput) {
        // Forlenger saker når det er 28 dager igjen til sluttdato (Tilsvarende Arena)
        val datoForUtvidelse = now(clock).plusDays(VedtakslengdeService.ANTALL_DAGER_FØR_UTVIDELSE)
        val saker = hentKandidaterForUtvidelseAvVedtakslengde(datoForUtvidelse)

        log.info("Fant ${saker.size} kandidater for utvidelse av vedtakslengde per $datoForUtvidelse")
        saker
            .also { log.info("Oppretter jobber for alle saker som er aktuelle kandidator for utvidelse av vedtakslengde. Antall = ${it.size}, Saker = $it") }
            .forEach {
                flytJobbRepository.leggTil(JobbInput(OpprettBehandlingUtvidVedtakslengdeJobbUtfører).forSak(it.toLong()))
            }
    }

    // TODO: Må filtrere vekk de som allerede har blitt kjørt, men ikke kvalifiserte til reell utvidelse av vedtakslengde
    private fun hentKandidaterForUtvidelseAvVedtakslengde(datoForUtvidelse: LocalDate): Set<SakId> {
        val kandidater = vedtakslengdeService.hentSakerAktuelleForUtvidelseAvVedtakslengde(datoForUtvidelse)
            .map { vurderUtvidelseBehov(it) }
            .groupBy { it.utvidelse?.let { u -> u::class } }

        val ingenBehandlingSaker = kandidater[null].orEmpty()

        if (ingenBehandlingSaker.isNotEmpty()) {
            log.info("Følgende saker har ingen gjeldende vedtatt behandling. Saker: ${ingenBehandlingSaker.map { it.sakId }}")
        }

        val manuelleSaker = kandidater[VedtakslengdeUtvidelse.Manuell::class].orEmpty()
        if (manuelleSaker.isNotEmpty()) {
            log.error("Følgende saker trenger manuell utvidelse av vedtakslengde. Må følges opp! Saker: ${manuelleSaker.map { it.sakId }}")
        }

        val ingenRettighetSaker = kandidater[VedtakslengdeUtvidelse.IngenFremtidigBistandsbehovRettighet::class].orEmpty()
        if (ingenRettighetSaker.isNotEmpty()) {
            log.info("Følgende saker har ingen fremtidig bistandsbehovrettighet. Saker: ${ingenRettighetSaker.map { it.sakId }}")
        }

        return kandidater[VedtakslengdeUtvidelse.Automatisk::class].orEmpty()
            .map { it.sakId }
            .toSet()
    }

    private fun vurderUtvidelseBehov(sakId: SakId): KandidatForUtvidelse {
        val sisteGjeldendeBehandling = behandlingService.finnBehandlingMedSisteFattedeVedtak(sakId)
            ?: return KandidatForUtvidelse(sakId = sakId, utvidelse = null)

        // Bruker sisteGjeldendeBehandling.id både for behandlingId og forrigeBehandlingId fordi vi ser på gjeldende behandling
        val vedtakslengdeUtvidelse = vedtakslengdeService.hentNesteVedtakslengdeUtvidelse(
            behandlingId = sisteGjeldendeBehandling.id,
            forrigeBehandlingId = sisteGjeldendeBehandling.id,
        )

        return KandidatForUtvidelse(sakId = sakId, utvidelse = vedtakslengdeUtvidelse)
    }

    private data class KandidatForUtvidelse(
        val sakId: SakId,
        val utvidelse: VedtakslengdeUtvidelse?,
    )

    companion object : ProvidersJobbSpesifikasjon {
        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): JobbUtfører {
            return OpprettJobbUtvidVedtakslengdeJobbUtfører(
                behandlingService = BehandlingService(repositoryProvider, gatewayProvider),
                vedtakslengdeService = VedtakslengdeService(repositoryProvider, gatewayProvider),
                flytJobbRepository = repositoryProvider.provide(),
            )
        }

        override val type = "batch.OpprettJobbUtvidVedtakslengde"
        override val navn = "Opprett jobber for å utvide vedtakslengde for saker"
        override val beskrivelse = "Skal opprette jobber for å utvide vedtakslengde for saker"

        /**
         * Kjøres hver dag kl 05:00
         */
        override val cron = CronExpression.createWithoutSeconds("0 5 * * *")
    }
}