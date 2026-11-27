package no.nav.aap.meldeplikt

import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.Prioritet
import no.nav.aap.motor.ProvidersJobbSpesifikasjon
import no.nav.aap.motor.cron.CronExpression
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate

class OpprettJobbForFritakMeldepliktJobbUtfører(
    private val flytJobbRepository: FlytJobbRepository,
    private val sakRepository: SakRepository,
    private val clock: Clock = Clock.systemDefaultZone()
) : JobbUtfører {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(input: JobbInput) {
        if (ingenHelligdagsunntakPåEkstrakjøredager()) return

        val saker = sakRepository.finnSakerMedFritakMeldeplikt()

        log.info("Oppretter jobber for alle saker som skal undersøkes for fritak meldeplikt. Antall = ${saker.size}")
        saker.forEach {
            flytJobbRepository.leggTil(JobbInput(OpprettBehandlingFritakMeldepliktJobbUtfører).forSak(it.toLong()).medPrioritet(Prioritet.LAV))
        }
    }

    private fun ingenHelligdagsunntakPåEkstrakjøredager(): Boolean {
        // Skal kun kjøre ekstra dersom det er et helligdagstilfelle
        val idag = LocalDate.now(clock)
        val helligdagsUnntak = erHelligdagsUnntak(idag)
        val dagerMedEkstraKjøring = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY)

        return idag.dayOfWeek in (dagerMedEkstraKjøring) && !helligdagsUnntak
    }

    companion object : ProvidersJobbSpesifikasjon {
        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): JobbUtfører {
            return OpprettJobbForFritakMeldepliktJobbUtfører(
                flytJobbRepository = repositoryProvider.provide(),
                sakRepository = repositoryProvider.provide(),
            )
        }

        override val type = "batch.OpprettJobbForFritakMeldeplikt"

        override val navn = "Start jobb for å sjekke behov for revurdering pga fritak meldeplikt"

        override val beskrivelse = "Start jobb for å sjekke behov for revurdering pga fritak meldeplikt."

        /**
         * Kjøres på onsdager for å fange opp alle utbetalinger for de med fritak
         *
         * Kjøres ekstra mandag og tirsdag, for å fange opp eventuelle helligdagsunntak på kjøredato
         * På sikt bør vi få en litt mindre sløsende løsning.
         */
        override val cron = CronExpression.createWithoutSeconds("10 2 * * 1,2,3")
    }
}
