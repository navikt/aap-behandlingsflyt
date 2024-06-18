package no.nav.aap.motor

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.dbconnect.Row
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import no.nav.aap.verdityper.sakogbehandling.SakId
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

internal class JobbRepository(private val connection: DBConnection) {
    private val log = LoggerFactory.getLogger(JobbRepository::class.java)

    fun leggTil(jobbInput: JobbInput) {
        val oppgaveId = connection.executeReturnKey(
            """
            INSERT INTO JOBB 
            (sak_id, behandling_id, type, neste_kjoring, parameters, payload) VALUES (?, ?, ?, ?, ?, ?)
            """.trimIndent()
        ) {
            setParams {
                setLong(1, jobbInput.sakIdOrNull()?.toLong())
                setLong(2, jobbInput.behandlingIdOrNull()?.toLong())
                setString(3, jobbInput.type())
                setLocalDateTime(4, jobbInput.nesteKjøringTidspunkt())
                setProperties(5, jobbInput.properties)
                setString(6, jobbInput.payload)
            }
        }

        connection.execute(
            """
            INSERT INTO JOBB_HISTORIKK 
            (jobb_id, status) VALUES (?, ?)
            """.trimIndent()
        ) {
            setParams {
                setLong(1, oppgaveId)
                setEnumName(2, JobbStatus.KLAR)
            }
        }
        log.info("Planlagt kjøring av jobb[${jobbInput.type()}] med kjøring etter ${jobbInput.nesteKjøringTidspunkt()}")
    }

    internal fun plukkJobb(): JobbInput? {
        val plukketJobb = connection.queryFirstOrNull(
            """
            SELECT id, type, status, sak_id, behandling_id, neste_kjoring, parameters, payload, 
                (SELECT count(1) FROM JOBB_HISTORIKK h WHERE h.jobb_id = o.id AND h.status = '${JobbStatus.FEILET.name}') as antall_feil
            FROM JOBB o
            WHERE status = '${JobbStatus.KLAR.name}'
              AND neste_kjoring < ?
              AND NOT EXISTS
                (
                SELECT 1
                 FROM JOBB op
                 WHERE op.status = '${JobbStatus.FEILET.name}'
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
                mapJobb(row)
            }
        }

        if (plukketJobb == null) {
            return null
        }

        connection.execute(
            """
            INSERT INTO JOBB_HISTORIKK 
            (jobb_id, status) VALUES (?, ?)
            """.trimIndent()
        ) {
            setParams {
                setLong(1, plukketJobb.id)
                setEnumName(2, JobbStatus.PLUKKET)
            }
        }

        return plukketJobb
    }

    private fun mapJobb(row: Row): JobbInput {
        return JobbInput(JobbType.parse(row.getString("type")))
            .medId(row.getLong("id"))
            .medStatus(row.getEnum("status"))
            .forBehandling(
                row.getLongOrNull("sak_id")?.let(::SakId),
                row.getLongOrNull("behandling_id")?.let(::BehandlingId)
            )
            .medAntallFeil(row.getLong("antall_feil"))
            .medProperties(row.getPropertiesOrNull("parameters"))
            .medPayload(row.getStringOrNull("payload"))
    }

    internal fun markerKjørt(jobbInput: JobbInput) {
        connection.execute("UPDATE JOBB SET status = ? WHERE id = ? AND status = 'KLAR'") {
            setParams {
                setEnumName(1, JobbStatus.FERDIG)
                setLong(2, jobbInput.id)
            }
            setResultValidator {
                require(it == 1)
            }
        }

        connection.execute(
            """
            INSERT INTO JOBB_HISTORIKK 
            (jobb_id, status) VALUES (?, ?)
            """.trimIndent()
        ) {
            setParams {
                setLong(1, jobbInput.id)
                setEnumName(2, JobbStatus.FERDIG)
            }
        }
    }

    internal fun markerFeilet(jobbInput: JobbInput, exception: Throwable) {
        // Da denne kjører i en ny transaksjon bør oppgaven låses slik at den ikke plukkes på nytt mens det logges
        connection.queryFirst("SELECT id FROM JOBB WHERE id = ? FOR UPDATE") {
            setParams {
                setLong(1, jobbInput.id)
            }
            setRowMapper {
                it.getLong("id")
            }
        }

        if (jobbInput.skalMarkeresSomFeilet()) {
            connection.execute("UPDATE JOBB SET status = ? WHERE id = ? AND status = 'KLAR'") {
                setParams {
                    setEnumName(1, JobbStatus.FEILET)
                    setLong(2, jobbInput.id)
                }
                setResultValidator {
                    require(it == 1)
                }
            }
        }

        connection.execute(
            """
            INSERT INTO JOBB_HISTORIKK 
            (jobb_id, status, feilmelding) VALUES (?, ?, ?)
            """.trimIndent()
        ) {
            setParams {
                setLong(1, jobbInput.id)
                setEnumName(2, JobbStatus.FEILET)
                setString(3, exception.stackTraceToString())
            }
        }
    }
}
