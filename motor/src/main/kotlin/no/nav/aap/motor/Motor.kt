package no.nav.aap.motor

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.dbconnect.transaction
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.Future
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

    // Benytter virtuals threads istedenfor plattform tråder
    private val executor = Executors.newThreadPerTaskExecutor(
        Thread.ofVirtual()
            .name("forbrenningskammer-", 1L)
            .factory()
    )
    private val watchdogExecutor = Executors.newScheduledThreadPool(1) as ScheduledThreadPoolExecutor

    private var stopped = false
    private val workers = HashMap<Int, Future<*>>()
    private var lastWatchdogLog = LocalDateTime.now()

    fun start() {
        log.info("Starter prosessering av oppgaver")
        IntRange(1, antallKammer).forEach { i ->
            val kammer = Forbrenningskammer(dataSource)
            workers[i] = executor.submit(kammer) // Legger inn en liten spread så det ikke pumpes på tabellen likt
            if (i != antallKammer) {
                Thread.sleep(100)
            }
        }
        log.info("Startet prosessering av oppgaver")
        watchdogExecutor.schedule(Watchdog(), 1, TimeUnit.MINUTES)
    }

    fun stop() {
        log.info("Avslutter prosessering av oppgaver")
        stopped = true
        watchdogExecutor.shutdownNow()
        executor.awaitTermination(10L, TimeUnit.SECONDS)
    }

    inner class Forbrenningskammer(private val dataSource: DataSource) : Runnable {
        private val log = LoggerFactory.getLogger(Forbrenningskammer::class.java)

        private var plukker = true
        override fun run() {
            while (!stopped) {
                log.debug("Starter plukking av oppgaver")
                try {
                    while (plukker) {
                        dataSource.transaction { connection ->
                            val repository = OppgaveRepository(connection)
                            val plukketOppgave = repository.plukkOppgave()
                            if (plukketOppgave != null) {
                                utførOppgave(plukketOppgave, connection)
                            }

                            if (plukker && plukketOppgave == null) {
                                plukker = false
                            }
                        }
                    }
                } catch (excetion: Throwable) {
                    log.warn("Feil under plukking av oppgaver", excetion)
                }
                log.debug("Ingen flere oppgaver å plukke, hviler litt")
                Thread.sleep(500)
                plukker = true
            }
        }

        private fun utførOppgave(oppgaveInput: OppgaveInput, connection: DBConnection) {
            try {
                dataSource.transaction { nyConnection ->
                    setteLogginformasjonForOppgave(connection, oppgaveInput)

                    log.info("Starter på oppgave :: {}", oppgaveInput.toString())

                    oppgaveInput.oppgave.konstruer(nyConnection).utfør(oppgaveInput)

                    log.info("Fullført oppgave :: {}", oppgaveInput.toString())
                    if (oppgaveInput.erScheduledOppgave()) {
                        OppgaveRepository(nyConnection).leggTil(
                            oppgaveInput.medNesteKjøring(
                                oppgaveInput.cron()!!.nextLocalDateTimeAfter(
                                    LocalDateTime.now()
                                )
                            )
                        )
                    }
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
            } finally {
                MDC.clear()
            }
        }

        private fun setteLogginformasjonForOppgave(
            connection: DBConnection,
            oppgaveInput: OppgaveInput
        ) {
            MDC.put("oppgaveid", "" + oppgaveInput.id)
            MDC.put("oppgavetype", oppgaveInput.type())
            MDC.put("sakId", oppgaveInput.sakIdOrNull().toString())
            MDC.put("behandlingId", oppgaveInput.behandlingIdOrNull().toString())
            MDC.put("callId", UUID.randomUUID().toString())

            val logInformasjon = LogInfoRepository(connection).hentInfor(
                oppgaveInput.sakIdOrNull(),
                oppgaveInput.behandlingIdOrNull()
            )
            if (logInformasjon != null) {
                MDC.put("saksnummer", logInformasjon.saksnummer)
                if (logInformasjon.referanse != null) {
                    MDC.put("behandlingReferanse", logInformasjon.referanse)
                }
            }
        }
    }

    /**
     * Watchdog som sjekker om alle workers kjører
     */
    inner class Watchdog : Runnable {
        val logger = LoggerFactory.getLogger(Watchdog::class.java)
        override fun run() {
            logger.debug("Sjekker status på workers")
            try {
                val allRunning = workers.values.all { !it.isDone }

                if (!allRunning && !stopped) {
                    val nyeWorkers: MutableList<Pair<Int, Forbrenningskammer>> = mutableListOf()
                    workers.forEach { (key, value) ->
                        if (value.state() in setOf(Future.State.CANCELLED, Future.State.SUCCESS)) {
                            logger.info("Fant workers som uventet har stoppet [{}]", value)
                            nyeWorkers.addLast(Pair(key, Forbrenningskammer(dataSource)))
                        } else if (value.state() == Future.State.FAILED) {
                            logger.info(
                                "Fant workers som uventet har blitt terminert [{}]",
                                value,
                                value.exceptionNow()
                            )
                            nyeWorkers.addLast(Pair(key, Forbrenningskammer(dataSource)))
                        }
                    }
                    nyeWorkers.forEach {
                        workers[it.first] = executor.submit(it.second)
                    }
                } else if (!stopped) {
                    if (lastWatchdogLog.plusMinutes(30).isBefore(LocalDateTime.now())) {
                        logger.info("Alle workers OK")
                        lastWatchdogLog = LocalDateTime.now()
                    }
                }
            } catch (exception: Throwable) {
                logger.warn("Ukjent feil under watchdog aktivtet", exception)
            }
            watchdogExecutor.schedule(Watchdog(), 1, TimeUnit.MINUTES)
        }
    }
}
