package no.nav.aap.motor.retry

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbRepository
import no.nav.aap.motor.JobbStatus
import no.nav.aap.motor.JobbType
import no.nav.aap.motor.mapJobb
import no.nav.aap.motor.mapJobbInklusivFeilmelding
import java.time.LocalDateTime

internal class RetryFeiledeJobberRepository(private val connection: DBConnection) {

    private val oppgaverRepository: JobbRepository = JobbRepository(connection)

    internal fun markerAlleFeiledeForKlare(): Int {
        val historikk = """
            INSERT INTO JOBB_HISTORIKK (jobb_id, status)
            SELECT id, 'KLAR' FROM JOBB WHERE status = 'FEILET'
        """.trimIndent()

        connection.execute(historikk)

        val query = """
                UPDATE JOBB SET status = 'KLAR' WHERE status = 'FEILET'
            """.trimIndent()
        var antallRader = 0
        connection.execute(query) {
            setResultValidator {
                antallRader = it
            }
        }

        return antallRader
    }

    internal fun markerFeiledeForKlare(jobbId: Long): Int {
        val historikk = """
            INSERT INTO JOBB_HISTORIKK (jobb_id, status)
            SELECT id, 'KLAR' FROM JOBB WHERE status = 'FEILET' AND id = ?
        """.trimIndent()

        connection.execute(historikk) {
            setParams {
                setLong(1, jobbId)
            }
        }

        val query = """
                UPDATE JOBB SET status = 'KLAR' WHERE status = 'FEILET' AND id = ?
            """.trimIndent()
        var antallRader = 0
        connection.execute(query) {
            setParams {
                setLong(1, jobbId)
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
                SELECT * FROM JOBB WHERE type = ? and status != 'FERDIG'
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
            SELECT id, 'KLAR' FROM JOBB WHERE status = 'FEILET' and id = ?
        """.trimIndent()

        connection.execute(historikk) {
            setParams {
                setLong(1, oppgave.id)
            }
        }

        val query = """
                UPDATE JOBB SET status = 'KLAR' WHERE status = 'FEILET' and id = ?
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

    fun hentAlleFeilede(): List<Pair<JobbInput, String?>> {
        val query  = """
            SELECT *, 
            (SELECT count(1) FROM JOBB_HISTORIKK h WHERE h.jobb_id = j.id AND h.status = '${JobbStatus.FEILET.name}') as antall_feil,
             (SELECT feilmelding FROM JOBB_HISTORIKK WHERE jobb_id = j.id and status = '${JobbStatus.FEILET.name}' ORDER BY OPPRETTET_TID DESC LIMIT 1) as feilmelding
            FROM JOBB j WHERE status = 'FEILET'
        """.trimIndent()
        return connection.queryList(query) {
            setRowMapper { row ->
                mapJobbInklusivFeilmelding(row)
            }
        }
    }

    fun hentInfoOmSisteAvType(type: String): JobbInput {
        val query = """
            SELECT *, 
            (SELECT count(1) FROM JOBB_HISTORIKK h WHERE h.jobb_id = j.id AND h.status = '${JobbStatus.FEILET.name}') as antall_feil
            FROM JOBB j WHERE status = 'KLAR' and type = ?
        """.trimIndent()
        return connection.queryFirst(query) {
            setParams {
                setString(1, type)
            }
            setRowMapper {
                mapJobb(it)
            }
        }
    }

    fun hentInfoOmSiste(antall: Int): List<Pair<JobbInput, String?>> {
        val query  = """
            SELECT *, 
            (SELECT count(1) FROM JOBB_HISTORIKK h WHERE h.jobb_id = j.id AND h.status = '${JobbStatus.FEILET.name}') as antall_feil,
             (SELECT feilmelding FROM JOBB_HISTORIKK WHERE jobb_id = j.id and status = '${JobbStatus.FEILET.name}' ORDER BY OPPRETTET_TID DESC LIMIT 1) as feilmelding
            FROM JOBB j LIMIT ?
        """.trimIndent()

        return connection.queryList(query) {
            setParams {
                setInt(1, antall)
            }
            setRowMapper { row ->
                mapJobbInklusivFeilmelding(row)
            }
        }
    }

    inner class FeilhåndteringOppgaveStatus(val id: Long, val type: String, val status: JobbStatus)
}
