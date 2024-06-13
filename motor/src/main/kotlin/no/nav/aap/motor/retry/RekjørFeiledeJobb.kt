package no.nav.aap.motor.retry

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.cron.CronExpression
import org.slf4j.LoggerFactory

internal const val OPPGAVE_TYPE = "oppgave.retryFeilede"

internal class RekjørFeiledeJobb(private val repository: RetryFeiledeJobberRepository) : JobbUtfører {
    private val log = LoggerFactory.getLogger(RekjørFeiledeJobb::class.java)

    override fun utfør(input: JobbInput) {

        val feilendeOppgaverMarkertForRekjøring = repository.markerAlleFeiledeForKlare()
        log.info("Markert {} oppgaver for rekjøring", feilendeOppgaverMarkertForRekjøring)
    }

    companion object : Jobb {
        override fun konstruer(connection: DBConnection): JobbUtfører {
            return RekjørFeiledeJobb(RetryFeiledeJobberRepository(connection))
        }

        override fun type(): String {
            return OPPGAVE_TYPE
        }

        override fun cron(): CronExpression {
            return CronExpression.create("0 0,15,30,45 7-17,20 * * *")
        }
    }
}