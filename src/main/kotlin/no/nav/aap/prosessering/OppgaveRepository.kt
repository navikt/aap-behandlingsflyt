package no.nav.aap.prosessering

import org.slf4j.LoggerFactory
import java.util.*

object OppgaveRepository {
    private val log = LoggerFactory.getLogger(OppgaveRepository::class.java)

    private val oppgaver = LinkedList<Gruppe>()
    private val LOCK = Object()

    fun leggTil(gruppe: Gruppe) {
        synchronized(LOCK) {
            if (gruppe.oppgaver().isNotEmpty()) {
                oppgaver.add(gruppe)
                log.info("Planlagt prosessering av gruppe {}", gruppe)
            }
        }
    }

    fun plukk(): Gruppe? {
        synchronized(LOCK) {
            if (oppgaver.isEmpty()) {
                return null
            }
            return oppgaver.removeFirst()
        }
    }

    fun harOppgaver(): Boolean {
        return oppgaver.isNotEmpty()
    }
}