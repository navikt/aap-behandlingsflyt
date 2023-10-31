package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.dbstuff.DBConnection
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

class OppgaveRepository(private val connection: DBConnection) {
    private val log = LoggerFactory.getLogger(OppgaveRepository::class.java)

    fun leggTil(oppgaveInput: OppgaveInput) {
        val oppgave = """
            INSERT INTO OPPGAVE 
            (sak_id, behandling_id, type, neste_kjoring) VALUES (?, ?, ?, ?)
        """.trimIndent()

        val oppgaveId = connection.executeReturnKeys(oppgave) {
            setParams {
                setLong(1, oppgaveInput.sakIdOrNull())
                setLong(2, oppgaveInput.behandlingIdOrNull())
                setString(3, oppgaveInput.type())
                setLocalDateTime(4, LocalDateTime.now())
            }
        }.first()

        val historikk = """
            INSERT INTO OPPGAVE_HISTORIKK 
            (oppgave_id, status) VALUES (?, ?)
        """.trimIndent()

        connection.execute(historikk) {
            setParams {
                setLong(1, oppgaveId)
                setString(2, OppgaveStatus.KLAR.name)
            }
        }
        log.info("Planlagt kjøring av oppgave[${oppgaveInput.type()}]")
    }

    internal fun plukkOppgave(): OppgaveInput? {
        val query = """
            SELECT id, type, sak_id, behandling_id
            FROM (
                SELECT id, type, sak_id, behandling_id, neste_kjoring
                FROM OPPGAVE 
                WHERE status = 'KLAR' and neste_kjoring < ?
             )
             ORDER BY neste_kjoring ASC
             FOR UPDATE SKIP LOCKED
        """.trimIndent()

        val plukketOppgave = connection.queryFirstOrNull(query) {
            setParams {
                setLocalDateTime(1, LocalDateTime.now())
            }
            setRowMapper {
                OppgaveInput(OppgaveType.parse(it.getString("type")))
                    .medId(it.getLong("id"))
                    .forBehandling(it.getLongOrNull("sak_id"), it.getLongOrNull("behandling_id"))
            }
        }

        if (plukketOppgave == null) {
            return null
        }
        connection.execute("UPDATE OPPGAVE SET status = ? WHERE id = ?") {
            setParams {
                setString(1, OppgaveStatus.PLUKKET.name)
                setLong(2, plukketOppgave.id)
            }
        }

        val historikk = """
            INSERT INTO OPPGAVE_HISTORIKK 
            (oppgave_id, status) VALUES (?, ?)
        """.trimIndent()

        connection.execute(historikk) {
            setParams {
                setLong(1, plukketOppgave.id)
                setString(2, OppgaveStatus.KLAR.name)
            }
        }

        return plukketOppgave
    }

    internal fun markerKjørt(oppgaveInput: OppgaveInput) {
        connection.execute("UPDATE OPPGAVE SET status = ? WHERE id = ? AND status = 'PLUKKET'") {
            setParams {
                setString(1, OppgaveStatus.FERDIG.name)
                setLong(2, oppgaveInput.id)
            }
            setResultValidator {
                require(it == 1)
            }
        }

        val historikk = """
            INSERT INTO OPPGAVE_HISTORIKK 
            (oppgave_id, status) VALUES (?, ?)
        """.trimIndent()

        connection.execute(historikk) {
            setParams {
                setLong(1, oppgaveInput.id)
                setString(2, OppgaveStatus.FERDIG.name)
            }
        }
    }

    internal fun markerFeilet(oppgaveInput: OppgaveInput, exception: Throwable) {
        connection.execute("UPDATE OPPGAVE SET status = ? WHERE id = ? AND status = 'PLUKKET'") {
            setParams {
                setString(1, OppgaveStatus.FEILET.name)
                setLong(2, oppgaveInput.id)
            }
            setResultValidator {
                require(it == 1)
            }
        }

        val historikk = """
            INSERT INTO OPPGAVE_HISTORIKK 
            (oppgave_id, status, feilmelding) VALUES (?, ?, ?)
        """.trimIndent()

        connection.execute(historikk) {
            setParams {
                setLong(1, oppgaveInput.id)
                setString(2, OppgaveStatus.FEILET.name)
                setString(3, exception.message.orEmpty().take(3000))
            }
        }
    }

    internal fun markerKlar(oppgaveInput: OppgaveInput) {
        connection.execute("UPDATE OPPGAVE SET status = ? WHERE id = ? and status IN ('PLUKKET', 'FEILET')") {
            setParams {
                setString(1, OppgaveStatus.KLAR.name)
                setLong(2, oppgaveInput.id)
            }
            setResultValidator {
                require(it == 1)
            }
        }

        val historikk = """
            INSERT INTO OPPGAVE_HISTORIKK 
            (oppgave_id, status) VALUES (?, ?)
        """.trimIndent()

        connection.execute(historikk) {
            setParams {
                setLong(1, oppgaveInput.id)
                setString(2, OppgaveStatus.KLAR.name)
            }
        }
    }

    fun harOppgaver(): Boolean {
        val antall =
            connection.queryFirst("SELECT count(1) as antall FROM OPPGAVE WHERE status not in ('FERDIG', 'FEILET')") {
                setRowMapper {
                    it.getLong("antall") > 0
                }
            }
        return antall
    }
}