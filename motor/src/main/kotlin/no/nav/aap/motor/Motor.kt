package no.nav.aap.motor

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
    private val antallKammer: Int = 8,
    oppgaver: List<Oppgave>
) {

    init {
        for (oppgave in oppgaver) {
            OppgaveType.leggTil(oppgave)
        }
    }

    private val log = LoggerFactory.getLogger(Motor::class.java)

    private val executor = Executors.newScheduledThreadPool(antallKammer) as ScheduledThreadPoolExecutor

    private var stopped = false

    fun start() {
        log.info("Starter prosessering av oppgaver")
        IntRange(1, antallKammer).forEach {
            executor.schedule(Forbrenningskammer(dataSource), 15L * it, TimeUnit.MILLISECONDS) // Legger inn en liten spread så det ikke pumpes på tabellen likt
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
                dataSource.transaction { nyConnection ->
                    MDC.put("oppgavetype", oppgaveInput.type())
                    MDC.put("sakId", oppgaveInput.sakIdOrNull().toString())
                    MDC.put("behandlingId", oppgaveInput.behandlingIdOrNull().toString())

                    log.info("Starter på oppgave")

                    oppgaveInput.oppgave.utfør(nyConnection, oppgaveInput)

                    log.info("Fullført oppgave")
                }
                OppgaveRepository(connection).markerKjørt(oppgaveInput)
            } catch (exception: Throwable) {
                // Kjører feil
                log.warn(
                    "Feil under prosessering av oppgave {}",
                    oppgaveInput,
                    exception
                )
                OppgaveRepository(connection).markerFeilet(oppgaveInput, exception)
            }
            MDC.clear()
        }

    }
}

internal class WrappedOppgaveException(val oppgaveInput: OppgaveInput, val exception: Throwable) : RuntimeException()
