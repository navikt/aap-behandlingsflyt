package no.nav.aap.behandlingsflyt.prosessering

import kotlinx.coroutines.Runnable
import org.slf4j.LoggerFactory
import javax.sql.DataSource

class Motor(
    private val dataSource: DataSource
) {

    private val log = LoggerFactory.getLogger(Motor::class.java)
    private val worker = Worker()
    private val workerThread = Thread(worker)

    fun start() {
        log.info("Starter prosessering av oppgaver")
        if (!workerThread.isAlive) {
            workerThread.start()
            log.info("Startet prosessering av oppgaver {}", workerThread)
        }
    }

    fun stop() {
        worker.stop()
    }

    fun harOppgaver(): Boolean {
        return OppgaveRepository.harOppgaver() || worker.utførerOppgave()
    }

    private inner class Worker : Runnable {
        private val log = LoggerFactory.getLogger(Worker::class.java)
        private val repo = OppgaveRepository
        private var running = false
        private var gruppe: Gruppe? = null

        override fun run() {
            running = true
            gruppe = repo.plukk()
            while (running) {
                if (gruppe != null) {
                    val utførteOppgaver = mutableListOf<String>()
                    try {
                        log.info("[{} - {}}] Plukket gruppe {}", gruppe!!.sakId(), gruppe!!.behandlingId(), gruppe)
                        for (oppgaveInput in gruppe!!.oppgaver()) {
                            log.info(
                                "[{} - {}}] Starter på oppgave '{}'",
                                oppgaveInput.sakId(),
                                oppgaveInput.behandlingId(),
                                oppgaveInput.type()
                            )
                            oppgaveInput.oppgave.utfør(oppgaveInput)
                            log.info(
                                "[{} - {}}] Fullført oppgave '{}'",
                                oppgaveInput.sakId(),
                                oppgaveInput.behandlingId(),
                                oppgaveInput.type()
                            )
                            utførteOppgaver.add(oppgaveInput.type())
                        }
                    } catch (exception: Exception) {
                        val nyGruppe = Gruppe()
                        gruppe!!.oppgaver()
                            .filter { oppgave -> utførteOppgaver.any { uo -> oppgave.type() == uo } }
                            .forEach { nyGruppe.leggTil(it) }
                        log.warn("Feil under prosessering av gruppe {}, gjenstående opgaver {}", gruppe, nyGruppe, exception)
                        repo.leggTil(gruppe = nyGruppe)
                    }
                }
                gruppe = repo.plukk()
                if (running && !repo.harOppgaver() && gruppe == null) {
                    Thread.sleep(500L)
                }
            }
            log.info("Stoppet prosessering {}", workerThread)
        }

        fun stop() {
            running = false
        }

        fun utførerOppgave(): Boolean {
            return gruppe != null
        }
    }
}