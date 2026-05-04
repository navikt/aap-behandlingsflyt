package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.behandling.gregulering.GReguleringService
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.Grunnbeløp
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
import java.time.Year

/**
 * Kjøres daglig og oppretter jobber for G-Regulering. Jobber opprettes for saksbehandlingskandidater hvor innvilget
 * AAP-periode passerer G-justeringen lagt inn i lista i Grunnbeløp.kt det siste året siden kjøre-dato og etter
 * Kelvin ble lansert i produksjon (altså etter G-justeringen 1.mai 2025). G-regulering medfører omberegninger, slik
 * at ytelsen blir korrekt både før og etter G-justering.
 *
 * Denne utplukk-jobben er tenkt å være deterministisk og re-kjørbar. Dvs. skal kunne kjøres med datoIdag/kjøredato
 * tilbake i tid og få samme resultat, og skal kunne kjøres flere ganger på samme dato uten å opprette duplikate jobber
 * for samme sak. Det burde kanskje også være mulig å tvinge omberegning ved å re-sette en status
 * (eks. G_REGULERING_2026 ??) på behandlingen og så kjøre jobben. Men her er omfanget ikke landet. Kan også være en
 * utvidelse til 2027 avhengig av tid, men lagt tilrette for i 2026 innføringen av jobben f.eks.
 */
class OpprettJobbForGReguleringJobbUtfører(
    private val behandlingService: BehandlingService,
    private val gReguleringService: GReguleringService,
    private val flytJobbRepository: FlytJobbRepository,
    private val clock: Clock = Clock.systemDefaultZone()
) : JobbUtfører {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(input: JobbInput) {
        val årIDag = Year.now(clock)
        val aktuellGJustering = hentAktuellGJustering(årIDag)
        if (aktuellGJustering == null || aktuellGJustering.dato.isAfter(LocalDate.of(2025, 5, 1))) {
            log.info("Avslutter søk etter G-reguleringskandidater. Ingen G-justering for år: ${årIDag} funnet i Grunnbeløp.kt")
            return
        }
        val saker = hentKandidaterForGRegulering(aktuellGJustering.dato)

        log.info("Fant ${saker.size} kandidater for G-regulering for gitt G-justering (?) ${aktuellGJustering?.dato}")
        saker
            .also { log.info("Oppretter jobber for alle saker som er aktuelle kandidater for G-regulering. Antall = ${it.size}, Saker = $it") }
            .forEach {
                // TODO: Lag ny OpprettBehandlingForGReguleringJobbUtfører..
                flytJobbRepository.leggTil(JobbInput(OpprettBehandlingUtvidVedtakslengdeJobbUtfører).forSak(it.toLong()))
            }
    }

    private fun hentAktuellGJustering(år: Year) : Grunnbeløp.GrunnbeløpDto? {
        return gReguleringService.finnesGrunnbeløpForÅr(år)
    }

    private fun hentKandidaterForGRegulering(datoForGJustering: LocalDate): Set<SakId> {
        val alleSaker = gReguleringService.hentSakerMedAktuellGJustering(datoForGJustering)

        // TODO: Filtrer saker

        return alleSaker
    }

    companion object : ProvidersJobbSpesifikasjon {
        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): JobbUtfører {
            return OpprettJobbForGReguleringJobbUtfører(
                behandlingService = BehandlingService(repositoryProvider, gatewayProvider),
                gReguleringService = GReguleringService(repositoryProvider, gatewayProvider),
                flytJobbRepository = repositoryProvider.provide(),
            )
        }

        override val type = "batch.OpprettJobbForGRegulering"
        override val navn = "Opprett jobber for G-regulering av saker grunnet årlig G-justering"
        override val beskrivelse = "Skal opprette jobber for omberegning av ytelse i saker, G-regulering"

        /**
         * Kjøres hver dag kl 05:00
         */
        override val cron = CronExpression.createWithoutSeconds("0 5 * * *")
    }
}