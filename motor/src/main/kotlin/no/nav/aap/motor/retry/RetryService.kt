package no.nav.aap.motor.retry

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.motor.OppgaveStatus
import org.slf4j.LoggerFactory

class RetryService(connection: DBConnection) {
    private val log = LoggerFactory.getLogger(RetryService::class.java)
    private val repository = RetryFeiledeOppgaverRepository(connection)

    fun enable() {
        val planlagteFeilhåndteringOppgaver = repository.planlagteCronOppgaver()

        planlagteFeilhåndteringOppgaver.forEach { oppgave ->
            if (oppgave.status == OppgaveStatus.FERDIG) {
                repository.planleggNyKjøring(oppgave.type)
            } else if(oppgave.status == OppgaveStatus.FEILET) {
                repository.markerSomKlar(oppgave)
            }
        }
        log.info("Planlagt kjøring av feilhåndteringsOppgave")
    }
}