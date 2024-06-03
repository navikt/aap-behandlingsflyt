package no.nav.aap.motor.retry

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbRepository
import no.nav.aap.motor.JobbStatus
import no.nav.aap.motor.JobbType
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import java.time.LocalDateTime

internal class RetryFeiledeJobberRepository(private val connection: DBConnection) {

    private val oppgaverRepository: JobbRepository = JobbRepository(connection)

    internal fun markerAlleFeiledeForKlare(): Int {
        val historikk = """
            INSERT INTO JOBB_HISTORIKK (jobb_id, status)
            SELECT id, 'KLAR' FROM OPPGAVE WHERE status = 'FEILET'
        """.trimIndent()

        connection.execute(historikk)

        val query = """
                UPDATE OPPGAVE SET status = 'KLAR' WHERE status = 'FEILET'
            """.trimIndent()
        var antallRader = 0
        connection.execute(query) {
            setResultValidator {
                antallRader = it
            }
        }

        return antallRader
    }

    internal fun markerFeiledeForKlare(behandlingId: BehandlingId): Int {
        val historikk = """
            INSERT INTO JOBB_HISTORIKK (jobb_id, status)
            SELECT id, 'KLAR' FROM OPPGAVE WHERE status = 'FEILET' AND behandling_id = ?
        """.trimIndent()

        connection.execute(historikk) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
        }

        val query = """
                UPDATE OPPGAVE SET status = 'KLAR' WHERE status = 'FEILET' AND behandling_id = ?
            """.trimIndent()
        var antallRader = 0
        connection.execute(query) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setResultValidator {
                antallRader = it
            }
        }

        return antallRader
    }

    internal fun planlagteCronOppgaver(): List<FeilhåndteringOppgaveStatus> {
        return JobbType.cronTypes().flatMap { type -> hentStatusPåOppgave(type) }
    }

    private fun hentStatusPåOppgave(type: String): List<FeilhåndteringOppgaveStatus> {
        val query = """
                SELECT * FROM OPPGAVE WHERE type = ? and status != 'FERDIG'
            """.trimIndent()

        val queryList = connection.queryList(query) {
            setParams {
                setString(1, type)
            }
            setRowMapper {
                FeilhåndteringOppgaveStatus(it.getLong("id"), type, JobbStatus.valueOf(it.getString("status")))
            }
        }

        if (queryList.isEmpty()) {
            return listOf(FeilhåndteringOppgaveStatus(-1L, type, JobbStatus.FERDIG))
        }

        return queryList
    }

    internal fun markerSomKlar(oppgave: FeilhåndteringOppgaveStatus) {
        val historikk = """
            INSERT INTO JOBB_HISTORIKK (jobb_id, status)
            SELECT id, 'KLAR' FROM OPPGAVE WHERE status = 'FEILET' and id = ?
        """.trimIndent()

        connection.execute(historikk) {
            setParams {
                setLong(1, oppgave.id)
            }
        }

        val query = """
                UPDATE OPPGAVE SET status = 'KLAR' WHERE status = 'FEILET' and id = ?
            """.trimIndent()
        connection.execute(query) {
            setParams {
                setLong(1, oppgave.id)
            }
        }
    }

    internal fun planleggNyKjøring(type: String) {
        val oppgave = JobbType.parse(type)
        oppgaverRepository.leggTil(
            JobbInput(oppgave)
                .medNesteKjøring(requireNotNull(oppgave.cron()?.nextLocalDateTimeAfter(LocalDateTime.now())))
        )
    }

    inner class FeilhåndteringOppgaveStatus(val id: Long, val type: String, val status: JobbStatus)
}
