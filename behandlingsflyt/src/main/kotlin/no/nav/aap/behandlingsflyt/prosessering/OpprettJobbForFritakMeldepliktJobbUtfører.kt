package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.erHelligdagsUnntakPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProvidersJobbSpesifikasjon
import no.nav.aap.motor.cron.CronExpression
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

class OpprettJobbForFritakMeldepliktJobbUtfører(
    private val flytJobbRepository: FlytJobbRepository,
    private val sakRepository: SakRepository,
    private val clock: Clock = Clock.systemDefaultZone()
) : JobbUtfører {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(input: JobbInput) {
        // Skal kun kjøres mandager dersom det er et helligdagstilfelle
        val idag = LocalDate.now(clock)
        val mandagInneværendeUke = idag.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val søndagInneværendeUke = idag.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))

        val helligdagsPeriode = erHelligdagsUnntakPeriode(Periode(mandagInneværendeUke, søndagInneværendeUke))

        if (idag.dayOfWeek == DayOfWeek.MONDAY && !helligdagsPeriode) {
            return
        }

        val saker = sakRepository.finnSakerMedFritakMeldeplikt()

        log.info("Oppretter jobber for alle saker som skal undersøkes for fritak meldeplikt. Antall = ${saker.size}")
        saker.forEach {
            flytJobbRepository.leggTil(JobbInput(OpprettBehandlingFritakMeldepliktJobbUtfører).forSak(it.toLong()))
        }
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
         * Kjøres på både mandag og onsdag for å fange opp tidlig utbetaling i helligdager. På sikt bør vi få en litt
         * mindre sløsende løsning.
         */
        override val cron = CronExpression.createWithoutSeconds("10 2 * * 1,3")
    }
}
