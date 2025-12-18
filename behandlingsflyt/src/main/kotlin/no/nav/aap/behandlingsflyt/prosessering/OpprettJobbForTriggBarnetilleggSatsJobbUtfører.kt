package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProvidersJobbSpesifikasjon
import no.nav.aap.motor.cron.CronExpression
import org.slf4j.LoggerFactory
import java.time.LocalDate

class OpprettJobbForTriggBarnetilleggSatsJobbUtfører(
    val sakRepository: SakRepository,
    private val flytJobbRepository: FlytJobbRepository
) : JobbUtfører {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(input: JobbInput) {

        val saker = sakRepository.finnSakerMedBarnetillegg(LocalDate.of(2026, 1, 1))
        log.info("Fant ${saker.size} saker med barnetillegg.")

        saker.forEach {
            flytJobbRepository.leggTil(JobbInput(TriggBarnetilleggSatsJobbUtfører).forSak(it.toLong()))
        }
    }


    companion object : ProvidersJobbSpesifikasjon {
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
}