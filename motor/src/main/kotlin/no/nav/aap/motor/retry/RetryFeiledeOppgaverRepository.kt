package no.nav.aap.motor.retry

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.motor.OppgaveInput
import no.nav.aap.motor.OppgaveRepository
import no.nav.aap.motor.OppgaveStatus
import java.time.LocalDateTime

internal class RetryFeiledeOppgaverRepository(private val connection: DBConnection) {

    private val oppgaverRepository: OppgaveRepository = OppgaveRepository(connection)

    internal fun markerAlleFeiledeForKlare(): Int {
        val historikk = """
            INSERT INTO OPPGAVE_HISTORIKK (oppgave_id, status)
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

    internal fun planlagteFeilhåndteringOppgaver(): List<FeilhåndteringOppgaveStatus> {
        val query = """
                SELECT * FROM OPPGAVE WHERE type = ? and status != 'FERDIG'
            """.trimIndent()

        val queryList = connection.queryList(query) {
            setParams {
                setString(1, OPPGAVE_TYPE)
            }
            setRowMapper {
                FeilhåndteringOppgaveStatus(it.getLong("id"), OppgaveStatus.valueOf(it.getString("status")))
            }
        }
        return queryList
    }

    internal fun markerSomKlar(oppgave: FeilhåndteringOppgaveStatus) {
        val historikk = """
            INSERT INTO OPPGAVE_HISTORIKK (oppgave_id, status)
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

    internal fun planleggNyKjøring(localDateTime: LocalDateTime) {
        oppgaverRepository.leggTil(
            OppgaveInput(RekjørFeiledeOppgaver)
                .medNesteKjøring(localDateTime)
        )
    }

    inner class FeilhåndteringOppgaveStatus(val id: Long, val status: OppgaveStatus)
}
