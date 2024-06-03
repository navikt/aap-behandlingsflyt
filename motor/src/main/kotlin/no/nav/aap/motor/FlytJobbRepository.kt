package no.nav.aap.motor

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.dbconnect.Row
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import no.nav.aap.verdityper.sakogbehandling.SakId
import org.slf4j.LoggerFactory

class FlytJobbRepository(private val connection: DBConnection) {
    private val log = LoggerFactory.getLogger(FlytJobbRepository::class.java)

    fun leggTil(jobbInput: JobbInput) {
        val oppgaveId = connection.executeReturnKey(
            """
            INSERT INTO JOBB 
            (sak_id, behandling_id, type, neste_kjoring) VALUES (?, ?, ?, ?)
            """.trimIndent()
        ) {
            setParams {
                setLong(1, jobbInput.sakIdOrNull()?.toLong())
                setLong(2, jobbInput.behandlingIdOrNull()?.toLong())
                setString(3, jobbInput.type())
                setLocalDateTime(4, jobbInput.nesteKjøringTidspunkt())
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
        log.info("Planlagt kjøring av oppgave[${jobbInput.type()}] med kjøring etter ${jobbInput.nesteKjøringTidspunkt()}")
    }


    private fun mapOppgave(row: Row): JobbInput {
        return JobbInput(JobbType.parse(row.getString("type")))
            .medId(row.getLong("id"))
            .medStatus(row.getEnum("status"))
            .forBehandling(
                row.getLongOrNull("sak_id")?.let(::SakId),
                row.getLongOrNull("behandling_id")?.let(::BehandlingId)
            )
            .medAntallFeil(row.getLong("antall_feil"))
    }

    fun hentOppgaveForBehandling(id: BehandlingId): List<JobbInput> {
        val query = """
            SELECT *, (SELECT count(1) FROM JOBB_HISTORIKK h WHERE h.jobb_id = op.id AND h.status = '${JobbStatus.FEILET.name}') as antall_feil
                 FROM JOBB op
                 WHERE op.status IN ('${JobbStatus.KLAR.name}','${JobbStatus.FEILET.name}')
                   AND op.behandling_id = ?
        """.trimIndent()

        return connection.queryList(query) {
            setParams {
                setLong(1, id.toLong())
            }
            setRowMapper { row ->
                mapOppgave(row)
            }
        }
    }

    fun harOppgaver(): Boolean {
        val antall =
            connection.queryFirst(
                "SELECT count(1) as antall " +
                        "FROM JOBB " +
                        "WHERE status not in ('${JobbStatus.FERDIG.name}', '${JobbStatus.FEILET.name}')"
            ) {
                setRowMapper {
                    it.getLong("antall") > 0
                }
            }
        return antall
    }
}
