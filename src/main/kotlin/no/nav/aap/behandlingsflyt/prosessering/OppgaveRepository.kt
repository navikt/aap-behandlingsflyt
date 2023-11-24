package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.dbconnect.Row
import no.nav.aap.behandlingsflyt.sak.SakId
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

class OppgaveRepository(private val connection: DBConnection) {
    private val log = LoggerFactory.getLogger(OppgaveRepository::class.java)

    fun leggTil(oppgaveInput: OppgaveInput) {
        val oppgaveId = connection.executeReturnKey(
            """
            INSERT INTO OPPGAVE 
            (sak_id, behandling_id, type, neste_kjoring) VALUES (?, ?, ?, ?)
            """.trimIndent()
        ) {
            setParams {
                setLong(1, oppgaveInput.sakIdOrNull()?.toLong())
                setLong(2, oppgaveInput.behandlingIdOrNull()?.toLong())
                setString(3, oppgaveInput.type())
                setLocalDateTime(4, oppgaveInput.nesteKjøringTidspunkt())
            }
        }

        connection.execute(
            """
            INSERT INTO OPPGAVE_HISTORIKK 
            (oppgave_id, status) VALUES (?, ?)
            """.trimIndent()
        ) {
            setParams {
                setLong(1, oppgaveId)
                setEnumName(2, OppgaveStatus.KLAR)
            }
        }
        log.info("Planlagt kjøring av oppgave[${oppgaveInput.type()}] med kjøring etter ${oppgaveInput.nesteKjøringTidspunkt()}")
    }

    internal fun plukkOppgave(): OppgaveInput? {
        val plukketOppgave = connection.queryFirstOrNull(
            """
            SELECT id, type, sak_id, behandling_id, neste_kjoring, 
                (SELECT count(1) FROM oppgave_historikk h WHERE h.oppgave_id = o.id AND h.status = '${OppgaveStatus.FEILET.name}') as antall_feil
            FROM OPPGAVE o
            WHERE status = '${OppgaveStatus.KLAR.name}'
              AND neste_kjoring < ?
              AND NOT EXISTS
                (
                SELECT 1
                 FROM OPPGAVE op
                 WHERE op.status = '${OppgaveStatus.FEILET.name}'
                   AND op.sak_id is not null
                   AND o.sak_id is not null
                   AND o.sak_id = op.sak_id
                   AND (o.behandling_id = op.behandling_id OR op.behandling_id IS NULL OR o.behandling_id IS NULL)
                )
            ORDER BY neste_kjoring ASC
            FOR UPDATE SKIP LOCKED
            LIMIT 1
            """.trimIndent()
        ) {
            setParams {
                setLocalDateTime(1, LocalDateTime.now())
            }
            setRowMapper { row ->
                mapOppgave(row)
            }
        }

        if (plukketOppgave == null) {
            return null
        }

        connection.execute(
            """
            INSERT INTO OPPGAVE_HISTORIKK 
            (oppgave_id, status) VALUES (?, ?)
            """.trimIndent()
        ) {
            setParams {
                setLong(1, plukketOppgave.id)
                setEnumName(2, OppgaveStatus.PLUKKET)
            }
        }

        return plukketOppgave
    }

    private fun mapOppgave(row: Row): OppgaveInput {
        return OppgaveInput(OppgaveType.parse(row.getString("type")))
            .medId(row.getLong("id"))
            .forBehandling(
                row.getLongOrNull("sak_id")?.let(::SakId),
                row.getLongOrNull("behandling_id")?.let(::BehandlingId)
            )
            .medAntallFeil(row.getLong("antall_feil"))
    }

    internal fun markerKjørt(oppgaveInput: OppgaveInput) {
        connection.execute("UPDATE OPPGAVE SET status = ? WHERE id = ? AND status = 'KLAR'") {
            setParams {
                setEnumName(1, OppgaveStatus.FERDIG)
                setLong(2, oppgaveInput.id)
            }
            setResultValidator {
                require(it == 1)
            }
        }

        connection.execute(
            """
            INSERT INTO OPPGAVE_HISTORIKK 
            (oppgave_id, status) VALUES (?, ?)
            """.trimIndent()
        ) {
            setParams {
                setLong(1, oppgaveInput.id)
                setEnumName(2, OppgaveStatus.FERDIG)
            }
        }
    }

    internal fun markerFeilet(oppgaveInput: OppgaveInput, exception: Throwable) {
        // Da denne kjører i en ny transaksjon bør oppgaven låses slik at den ikke plukkes på nytt mens det logges
        connection.queryFirst("SELECT id FROM OPPGAVE WHERE id = ? FOR UPDATE") {
            setParams {
                setLong(1, oppgaveInput.id)
            }
            setRowMapper {
                it.getLong("id")
            }
        }

        if (oppgaveInput.skalMarkeresSomFeilet()) {
            connection.execute("UPDATE OPPGAVE SET status = ? WHERE id = ? AND status = 'KLAR'") {
                setParams {
                    setEnumName(1, OppgaveStatus.FEILET)
                    setLong(2, oppgaveInput.id)
                }
                setResultValidator {
                    require(it == 1)
                }
            }
        }

        connection.execute(
            """
            INSERT INTO OPPGAVE_HISTORIKK 
            (oppgave_id, status, feilmelding) VALUES (?, ?, ?)
            """.trimIndent()
        ) {
            setParams {
                setLong(1, oppgaveInput.id)
                setEnumName(2, OppgaveStatus.FEILET)
                setString(3, exception.message.orEmpty())
            }
        }
    }

    fun harOppgaver(): Boolean {
        val antall =
            connection.queryFirst(
                "SELECT count(1) as antall " +
                        "FROM OPPGAVE " +
                        "WHERE status not in ('${OppgaveStatus.FERDIG.name}', '${OppgaveStatus.FEILET.name}')"
            ) {
                setRowMapper {
                    it.getLong("antall") > 0
                }
            }
        return antall
    }
}
