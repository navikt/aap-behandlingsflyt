package no.nav.aap.behandlingsflyt.prosessering

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
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Denne jobben brukes til å automatisk sende ut brev ved endring av barnetillegg-sats. Jobben er konfigurert til å
 * kjøre 2.januar kl 09:00 hvert år. Dersom kjøringen skal ha effekt må følgende gjøres:
 * - Verifiser at brevet som sendes er oppdatert med riktig innhold
 * - Oppdater [OpprettJobbForTriggBarnetilleggSatsJobbUtfører.jobbKonfigurasjon]
 *   - [OpprettJobbForTriggBarnetilleggSatsJobbUtfører.JobbKonfigurasjon.unikBrevreferanseForSak] må endres for at det
 *     skal sendes et nytt brev. Man kan f.eks. bruke et datostempel for dagen brevet skal sendes, eller dato satsen
 *     endres. Hensikten med variabelen er å ikke sende duplikate brev.
 *   - [OpprettJobbForTriggBarnetilleggSatsJobbUtfører.JobbKonfigurasjon.erAktiv] må settes til `true`
 *
 * Etter at jobben er kjørt og verifisert OK kan
 * [OpprettJobbForTriggBarnetilleggSatsJobbUtfører.JobbKonfigurasjon.erAktiv] settes tilbake til `false`.
 */
class OpprettJobbForTriggBarnetilleggSatsJobbUtfører(
    val sakRepository: SakRepository,
    private val flytJobbRepository: FlytJobbRepository
) : JobbUtfører {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(input: JobbInput) {

        if (!jobbKonfigurasjon.erAktiv) {
            log.warn(
                "Jobb OpprettJobbForTriggBarnetilleggSatsJobbUtfører er ikke aktiv." +
                        "Jobben må konfigureres og settes til aktiv dersom det skal opprettes nye behandlinger om endring av barnetillegg-sats for saker med barnetillegg."
            )
            return
        }

        // forutsetter at jobb kjører 2. januar hvert år
        val førsteDagMedNyttBarnetillegg = LocalDate.now().withDayOfYear(1)
        val saker = sakRepository.finnSakerMedBarnetillegg(førsteDagMedNyttBarnetillegg)
        log.info("Fant ${saker.size} saker med barnetillegg.")

        saker.forEach {
            flytJobbRepository.leggTil(JobbInput(TriggBarnetilleggSatsJobbUtfører).forSak(it.toLong()).medPrioritet(Prioritet.BAKGRUNN))
        }
    }

    companion object : ProvidersJobbSpesifikasjon {
        val formatter = DateTimeFormatter.ofPattern("ddMMyyyy")
        val datoForJobb = LocalDate.now().format(formatter)
        var jobbKonfigurasjon: JobbKonfigurasjon = JobbKonfigurasjon(
            erAktiv = false,
            unikBrevreferanseForSak = datoForJobb
        )

        override val type = "satsendring-lag-jobber"
        override val navn = "Finn saker med barnetillegg, og queue jobber for dem."
        override val beskrivelse = ""

        /**
         * Kjør hver 2 januar kl 09:00.
         */
        override val cron: CronExpression = CronExpression.createWithoutSeconds("0 9 2 1 *")

        override fun konstruer(
            repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider
        ): OpprettJobbForTriggBarnetilleggSatsJobbUtfører {
            return OpprettJobbForTriggBarnetilleggSatsJobbUtfører(
                sakRepository = repositoryProvider.provide(),
                flytJobbRepository = repositoryProvider.provide(),
            )
        }
    }

    data class JobbKonfigurasjon(val erAktiv: Boolean, val unikBrevreferanseForSak: String)
}