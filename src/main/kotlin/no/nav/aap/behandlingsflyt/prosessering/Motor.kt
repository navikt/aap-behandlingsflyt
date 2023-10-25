package no.nav.aap.behandlingsflyt.prosessering

import kotlinx.coroutines.Runnable
import no.nav.aap.behandlingsflyt.dbstuff.transaction
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.concurrent.Executors
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import javax.sql.DataSource

class Motor(
    private val dataSource: DataSource
) {

    private val log = LoggerFactory.getLogger(Motor::class.java)

    private val maksKøStørrelse = 20

    private val executor = Executors.newFixedThreadPool(5) as ThreadPoolExecutor
    private val pollingExecutor = Executors.newScheduledThreadPool(1)
    private var lastTaskPolled: LocalDateTime? = null
    private var lastTaskPolledLogged: LocalDateTime? = LocalDateTime.now()
    private var stopped = false

    fun start() {
        log.info("Starter prosessering av oppgaver")
        pollingExecutor.schedule(PollingWorker(dataSource), 0L, TimeUnit.SECONDS)
    }

    fun stop() {
        stopped = true
        pollingExecutor.shutdown()
        pollingExecutor.awaitTermination(5L, TimeUnit.SECONDS)
        executor.shutdown()
        executor.awaitTermination(10L, TimeUnit.SECONDS)
    }

    fun harOppgaverSomIkkeErProssessert(): Boolean {
        return OppgaveRepository.harOppgaver() || harOppgaverKjørende()
    }

    private fun harOppgaverKjørende(): Boolean {
        return executor.activeCount != 0
    }

    private inner class PollingWorker(val dataSource: DataSource) : Runnable {
        private val log = LoggerFactory.getLogger(PollingWorker::class.java)
        private val repo = OppgaveRepository
        private var running = false
        private var gruppe: Gruppe? = null

        override fun run() {
            running = true
            gruppe = plukk()
            while (running) {
                if (gruppe != null) {
                    executor.submit(OppgaveWorker(dataSource, gruppe!!))
                    lastTaskPolled = LocalDateTime.now()
                }
                gruppe = plukk()
                if (running && !repo.harOppgaver() && gruppe == null) {
                    running = false
                }
            }
            if (lastTaskPolled?.isBefore(LocalDateTime.now().minusMinutes(10)) == true
                && lastTaskPolledLogged?.isBefore(LocalDateTime.now().minusMinutes(10)) == true
            ) {

                log.info("Ikke nye plukket oppgaver siden {}", lastTaskPolled)
                lastTaskPolledLogged = LocalDateTime.now()
            }
            if (!stopped) {
                pollingExecutor.schedule(PollingWorker(dataSource), 500L, TimeUnit.MILLISECONDS)
            }
        }

        fun plukk(): Gruppe? {
            if (executor.queue.size >= maksKøStørrelse) {
                return null
            }
            return repo.plukk()
        }
    }

    private class OppgaveWorker(private val dataSource: DataSource, private val gruppe: Gruppe) : Runnable {
        private val log = LoggerFactory.getLogger(OppgaveWorker::class.java)
        private val repo = OppgaveRepository
        override fun run() {
            val utførteOppgaver = mutableListOf<String>()

            try {
                dataSource.transaction { connection ->
                    log.info("[{} - {}}] Plukket gruppe {}", gruppe.sakId(), gruppe.behandlingId(), gruppe)
                    for (oppgaveInput in gruppe.oppgaver()) {
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
                        utførteOppgaver.add(oppgaveInput.type())
                    }
                }
            } catch (exception: Throwable) {
                val nyGruppe = Gruppe()
                gruppe.oppgaver()
                    .filter { oppgave -> utførteOppgaver.any { uo -> oppgave.type() == uo } }
                    .forEach { nyGruppe.leggTil(it) }
                log.warn(
                    "Feil under prosessering av gruppe {}, gjenstående opgaver {}",
                    gruppe,
                    nyGruppe,
                    exception
                )
                repo.leggTil(gruppe = nyGruppe)
            }
        }
    }
}