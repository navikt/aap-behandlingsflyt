package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.behandling.vedtakslengde.VedtakslengdeService
import no.nav.aap.behandlingsflyt.faktagrunnlag.SakOgBehandlingService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.miljo.Miljø.erDev
import no.nav.aap.komponenter.miljo.Miljø.erLokal
import no.nav.aap.komponenter.miljo.Miljø.erProd
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

class OpprettJobbUtvidVedtakslengdeJobbUtfører(
    private val sakOgBehandlingService: SakOgBehandlingService,
    private val vedtakslengdeService: VedtakslengdeService,
    private val flytJobbRepository: FlytJobbRepository,
    private val unleashGateway: UnleashGateway,
    private val clock: Clock = Clock.systemDefaultZone()
) : JobbUtfører {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(input: JobbInput) {
        // Forlenger saker når det er 28 dager igjen til sluttdato
        val datoForUtvidelse = now(clock).plusDays(28)
        val saker = hentKandidaterForUtvidelseAvVedtakslengde(datoForUtvidelse)

        log.info("Fant ${saker.size} kandidater for utvidelse av vedtakslengde per $datoForUtvidelse")
        if (unleashGateway.isEnabled(BehandlingsflytFeature.UtvidVedtakslengdeJobb)) {
            saker
                // TODO fjerne dette filteret når vi har fått verifisert at jobben fungerer som forventet i dev/prod
                .filter {
                    if (erDev()) it.id == 4243L
                    else if (erProd()) it.id == 50L
                    else if (erLokal()) true
                    else false
                }
                .also { log.info("Oppretter jobber for alle saker som er aktuelle kandidator for utvidelse av vedtakslengde. Antall = ${it.size}") }
                .forEach {
                    flytJobbRepository.leggTil(JobbInput(OpprettBehandlingUtvidVedtakslengdeJobbUtfører).forSak(it.toLong()))
                }
        }
    }

    // TODO: Må filtrere vekk de som allerede har blitt kjørt, men ikke kvalifiserte til reell utvidelse av vedtakslengde
    private fun hentKandidaterForUtvidelseAvVedtakslengde(datoForUtvidelse: LocalDate): Set<SakId> {
        return vedtakslengdeService.hentSakerAktuelleForUtvidelseAvVedtakslengde(datoForUtvidelse)
            .filter { kunSakerMedBehovForUtvidelseAvVedtakslengde(it, datoForUtvidelse) }
            .toSet()
    }

    private fun kunSakerMedBehovForUtvidelseAvVedtakslengde(id: SakId, dato: LocalDate): Boolean {
        val sisteGjeldendeBehandling = sakOgBehandlingService.finnBehandlingMedSisteFattedeVedtak(id)
        if (sisteGjeldendeBehandling != null) {
            // Bruker sisteGjeldendeBehandling.id både for behandlingId og forrigeBehandlingId fordi vi ser på gjeldende behandling
            return vedtakslengdeService.skalUtvideSluttdato(sisteGjeldendeBehandling.id, sisteGjeldendeBehandling.id, dato)
        }
        return false
    }

    companion object : ProvidersJobbSpesifikasjon {
        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): JobbUtfører {
            return OpprettJobbUtvidVedtakslengdeJobbUtfører(
                sakOgBehandlingService = SakOgBehandlingService(repositoryProvider, gatewayProvider),
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