package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.behandling.vedtakslengde.VedtakslengdeUtvidelse
import no.nav.aap.behandlingsflyt.behandling.vedtakslengde.VedtakslengdeService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
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
    private val unleashGateway: UnleashGateway,
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
            .fold(KategoriserteKandidater()) { acc, sakId ->
                val utvidelse = vurderUtvidelseBehov(sakId)
                // Sikrer at nye typer i VedtakslengdeUtvidelse må håndteres
                when (utvidelse) {
                    is VedtakslengdeUtvidelse.Automatisk -> acc.copy(automatiske = acc.automatiske + sakId)
                    is VedtakslengdeUtvidelse.Manuell -> acc.copy(manuelle = acc.manuelle + sakId)
                    is VedtakslengdeUtvidelse.IngenFremtidigBistandsbehovRettighet -> acc.copy(ingenRettighet = acc.ingenRettighet + sakId)
                    null -> acc.copy(ingenBehandling = acc.ingenBehandling + sakId)
                }
            }

        if (kandidater.ingenBehandling.isNotEmpty()) {
            log.info("Følgende saker har ingen gjeldende vedtatt behandling. Saker: ${kandidater.ingenBehandling}")
        }
        if (kandidater.ingenRettighet.isNotEmpty()) {
            log.info("Følgende saker har ingen fremtidig bistandsbehovrettighet. Saker: ${kandidater.ingenRettighet}")
        }

        return if (unleashGateway.isEnabled(BehandlingsflytFeature.OpprettManuellVedtakslengdeBehandling)) {
            if (kandidater.manuelle.isNotEmpty()) {
                log.info("Følgende saker trenger manuell utvidelse av vedtakslengde. Saker: ${kandidater.manuelle}")
            }
            if (kandidater.automatiske.isNotEmpty()) {
                log.info("Følgende saker får automatisk utvidet vedtakslengde. Saker: ${kandidater.automatiske}")
            }
            kandidater.automatiske + kandidater.manuelle
        } else {
            if (kandidater.manuelle.isNotEmpty()) {
                log.error("Følgende saker trenger manuell utvidelse av vedtakslengde. Må følges opp! Saker: ${kandidater.manuelle}")
            }
            if (kandidater.automatiske.isNotEmpty()) {
                log.info("Følgende saker får automatisk utvidet vedtakslengde. Saker: ${kandidater.automatiske}")
            }
            kandidater.automatiske
        }
    }

    private fun vurderUtvidelseBehov(sakId: SakId): VedtakslengdeUtvidelse? {
        val sisteGjeldendeBehandling = behandlingService.finnBehandlingMedSisteFattedeVedtak(sakId)
            ?: return null

        // Bruker sisteGjeldendeBehandling.id både for behandlingId og forrigeBehandlingId fordi vi ser på gjeldende behandling
        return vedtakslengdeService.hentNesteVedtakslengdeUtvidelse(
            behandlingId = sisteGjeldendeBehandling.id,
            forrigeBehandlingId = sisteGjeldendeBehandling.id,
        )
    }

    private data class KategoriserteKandidater(
        val automatiske: Set<SakId> = emptySet(),
        val manuelle: Set<SakId> = emptySet(),
        val ingenRettighet: Set<SakId> = emptySet(),
        val ingenBehandling: Set<SakId> = emptySet(),
    )

    companion object : ProvidersJobbSpesifikasjon {
        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): JobbUtfører {
            return OpprettJobbUtvidVedtakslengdeJobbUtfører(
                behandlingService = BehandlingService(repositoryProvider, gatewayProvider),
                vedtakslengdeService = VedtakslengdeService(repositoryProvider, gatewayProvider),
                flytJobbRepository = repositoryProvider.provide(),
                unleashGateway = gatewayProvider.provide(),
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