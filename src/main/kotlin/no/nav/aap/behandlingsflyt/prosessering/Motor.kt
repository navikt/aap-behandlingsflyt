package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.dbconnect.transaction
import org.slf4j.LoggerFactory
import org.slf4j.MDC
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
            executor.schedule(Forbrenningskammer(dataSource), 15L * it, TimeUnit.MILLISECONDS)
        }
    }

    fun stop() {
        stopped = true
        executor.awaitTermination(10L, TimeUnit.SECONDS)
    }

    fun harOppgaverKjørende(): Boolean {
        return executor.activeCount != 0
    }

    inner class Forbrenningskammer(private val dataSource: DataSource) : Runnable {
        private val log = LoggerFactory.getLogger(Forbrenningskammer::class.java)

        private var running = true
        override fun run() {
            try {
                while (running) {
                    try {
                        dataSource.transaction { connection ->
                            val repository = OppgaveRepository(connection)
                            val plukketOppgave = repository.plukkOppgave()
                            if (plukketOppgave != null) {
                                utførOppgave(plukketOppgave, connection)
                            }

                            if (running && plukketOppgave == null) {
                                running = false
                            }
                        }
                    } catch (exception: WrappedOppgaveException) {
                        dataSource.transaction { connection ->
                            OppgaveRepository(connection).markerFeilet(exception.oppgaveInput, exception.exception)
                        }
                    }
                }
            } catch (excetion: Throwable) {
                log.warn("Feil under plukking av oppgaver", excetion)
            }
            if (!stopped) {
                executor.schedule(Forbrenningskammer(dataSource), 500L, TimeUnit.MILLISECONDS)
            }
        }

        private fun utførOppgave(oppgaveInput: OppgaveInput, connection: DBConnection) {
            try {
                MDC.put("oppgavetype", oppgaveInput.type())
                MDC.put("sakId", oppgaveInput.sakIdOrNull().toString())
                MDC.put("behandlingId", oppgaveInput.behandlingIdOrNull().toString())

                log.info("Starter på oppgave")

                oppgaveInput.oppgave.utfør(connection, oppgaveInput)

                log.info("Fullført oppgave")

                OppgaveRepository(connection).markerKjørt(oppgaveInput)
            } catch (exception: Throwable) {
                // Kjører feil
                log.warn(
                    "Feil under prosessering av oppgave {}",
                    oppgaveInput,
                    exception
                )
                throw WrappedOppgaveException(oppgaveInput, exception)
            }
            MDC.clear()
        }

    }
}

internal class WrappedOppgaveException(val oppgaveInput: OppgaveInput, val exception: Throwable) : RuntimeException()
