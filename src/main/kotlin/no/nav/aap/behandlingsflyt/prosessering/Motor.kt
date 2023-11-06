package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.dbstuff.DBConnection
import no.nav.aap.behandlingsflyt.dbstuff.transaction
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import javax.sql.DataSource

class Motor(
    private val dataSource: DataSource,
    private val workers: Int = 5
) {

    private val log = LoggerFactory.getLogger(Motor::class.java)

    private val executor = Executors.newScheduledThreadPool(workers) as ScheduledThreadPoolExecutor

    private var stopped = false

    fun start() {
        log.info("Starter prosessering av oppgaver")
        IntRange(1, workers).forEach {
            executor.schedule(OppgaveWorker(dataSource), 15L * it, TimeUnit.MILLISECONDS)
        }
    }

    fun stop() {
        stopped = true
        executor.awaitTermination(10L, TimeUnit.SECONDS)
    }

    fun harOppgaverKjørende(): Boolean {
        return executor.activeCount != 0
    }

    inner class OppgaveWorker(private val dataSource: DataSource) : Runnable {
        private val log = LoggerFactory.getLogger(OppgaveWorker::class.java)

        private var running = true
        override fun run() {
            try {
                while (running) {
                    dataSource.transaction { connection ->
                        val repository = OppgaveRepository(connection)
                        val oppgaveInput = repository.plukkOppgave()
                        if (oppgaveInput != null) {
                            utførOppgave(oppgaveInput, connection)
                        }

                        if (running && oppgaveInput == null) {
                            running = false
                        }
                    }
                }
            } catch (excetion: Throwable) {
                log.warn("Feil under plukking av oppgaver", excetion)
            }
            if (!stopped) {
                executor.schedule(OppgaveWorker(dataSource), 500L, TimeUnit.MILLISECONDS)
            }
        }

        private fun utførOppgave(oppgaveInput: OppgaveInput, connection: DBConnection) {
            try {
                log.info(
                    "[{} - {}}] Starter på oppgave '{}'",
                    oppgaveInput.sakId(),
                    oppgaveInput.behandlingId(),
                    oppgaveInput.type()
                )
                oppgaveInput.oppgave.utfør(connection, oppgaveInput)
                log.info(
                    "[{} - {}}] Fullført oppgave '{}'",
                    oppgaveInput.sakId(),
                    oppgaveInput.behandlingId(),
                    oppgaveInput.type()
                )
                OppgaveRepository(connection).markerKjørt(oppgaveInput)
            } catch (exception: Throwable) {
                OppgaveRepository(connection).markerFeilet(oppgaveInput, exception)
                log.warn(
                    "Feil under prosessering av oppgave {}",
                    oppgaveInput,
                    exception
                )
            }
        }

    }
}