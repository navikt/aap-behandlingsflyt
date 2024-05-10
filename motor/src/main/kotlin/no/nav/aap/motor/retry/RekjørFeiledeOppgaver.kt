package no.nav.aap.motor.retry

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.motor.CronExpression
import no.nav.aap.motor.Oppgave
import no.nav.aap.motor.OppgaveInput
import no.nav.aap.motor.OppgaveUtfører
import org.slf4j.LoggerFactory

internal const val OPPGAVE_TYPE = "oppgave.retryFeilede"

internal class RekjørFeiledeOppgaver(private val repository: RetryFeiledeOppgaverRepository) : OppgaveUtfører {
    private val log = LoggerFactory.getLogger(RekjørFeiledeOppgaver::class.java)

    override fun utfør(input: OppgaveInput) {

        val feilendeOppgaverMarkertForRekjøring = repository.markerAlleFeiledeForKlare()
        log.info("Markert {} oppgaver for rekjøring", feilendeOppgaverMarkertForRekjøring)
    }

    companion object : Oppgave {
        override fun konstruer(connection: DBConnection): OppgaveUtfører {
            return RekjørFeiledeOppgaver(RetryFeiledeOppgaverRepository(connection))
        }

        override fun type(): String {
            return OPPGAVE_TYPE
        }

        override fun cron(): CronExpression {
            return CronExpression.create("0 0,15,30,45 7-17,20 * * *")
        }
    }
}