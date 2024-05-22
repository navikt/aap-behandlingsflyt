package no.nav.aap.motor

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.dbconnect.Row
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import no.nav.aap.verdityper.sakogbehandling.SakId
import org.slf4j.LoggerFactory

class FlytOppgaveRepository(private val connection: DBConnection) {
    private val log = LoggerFactory.getLogger(FlytOppgaveRepository::class.java)

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


    private fun mapOppgave(row: Row): OppgaveInput {
        return OppgaveInput(OppgaveType.parse(row.getString("type")))
            .medId(row.getLong("id"))
            .medStatus(row.getEnum("status"))
            .forBehandling(
                row.getLongOrNull("sak_id")?.let(::SakId),
                row.getLongOrNull("behandling_id")?.let(::BehandlingId)
            )
            .medAntallFeil(row.getLong("antall_feil"))
    }

    fun hentOppgaveForBehandling(id: BehandlingId): List<OppgaveInput> {
        val query = """
            SELECT *, (SELECT count(1) FROM oppgave_historikk h WHERE h.oppgave_id = op.id AND h.status = '${OppgaveStatus.FEILET.name}') as antall_feil
                 FROM OPPGAVE op
                 WHERE op.status IN ('${OppgaveStatus.KLAR.name}','${OppgaveStatus.FEILET.name}')
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
