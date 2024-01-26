package no.nav.aap.motor.retry

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.motor.Oppgave
import no.nav.aap.motor.OppgaveInput
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

internal const val OPPGAVE_TYPE = "oppgave.retryFeilede"

object RekjørFeiledeOppgaver : Oppgave() {

    private val log = LoggerFactory.getLogger(RekjørFeiledeOppgaver::class.java)

    override fun utfør(connection: DBConnection, input: OppgaveInput) {
        val repository = RetryFeiledeOppgaverRepository(connection)

        val feilendeOppgaverMarkertForRekjøring = repository.markerAlleFeiledeForKlare()
        log.info("Markert {} oppgaver for rekjøring", feilendeOppgaverMarkertForRekjøring)

        repository.planleggNyKjøring(LocalDateTime.now().plusHours(1))
    }

    override fun type(): String {
        return OPPGAVE_TYPE
    }
}