package no.nav.aap.behandlingsflyt.hendelse.bruddaktivitetsplikt

import no.nav.aap.komponenter.dbconnect.DBConnection
import java.time.LocalDateTime
import java.util.*

class BruddAktivitetspliktRepository(private val connection: DBConnection) {
    fun lagreBruddAktivitetspliktHendelse(request: BruddAktivitetspliktRequest) {
        val query = """
            INSERT INTO BRUDD_AKTIVITETSPLIKT (SAKSNUMMER, BRUDD, PERIODE, BEGRUNNELSE, PARAGRAF, OPPRETTET_TID , HENDELSE_ID) VALUES (?, ?, ?::daterange, ?, ?, ?, ?)
            """.trimIndent()

        connection.executeBatch(query, request.perioder) {
            setParams { periode ->
                setString(1, request.saksnummer)
                setEnumName(2, request.brudd)
                setPeriode(3, periode)
                setString(4, request.begrunnelse)
                setEnumName(5, request.paragraf)
                setLocalDateTime(6, LocalDateTime.now())
                setUUID(7, UUID.randomUUID())
            }
        }
    }

    fun deleteAll() {
        connection.execute(
            """
                DELETE FROM BRUDD_AKTIVITETSPLIKT
            """.trimIndent()) {}
    }

    fun hentBruddAktivitetspliktHendelser(saksnummer: String) : List<BruddAktivitetspliktHendelseDto> {
        return connection.queryList(
            """
                SELECT *
                FROM BRUDD_AKTIVITETSPLIKT
                WHERE SAKSNUMMER = ?
            """.trimIndent()) {
            setParams { setString(1, saksnummer) }
            setRowMapper { row ->
                BruddAktivitetspliktHendelseDto(
                    brudd = row.getEnum("BRUDD"),
                    paragraf = row.getEnum("PARAGRAF"),
                    periode = row.getPeriode("PERIODE"),
                    begrunnelse = row.getString("BEGRUNNELSE"),
                    hendelseId = row.getUUID("HENDELSE_ID")
                )
            }
        }
    }
}