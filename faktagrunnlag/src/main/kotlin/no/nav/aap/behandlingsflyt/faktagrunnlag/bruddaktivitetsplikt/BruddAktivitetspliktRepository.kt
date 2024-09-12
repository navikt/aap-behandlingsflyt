package no.nav.aap.behandlingsflyt.faktagrunnlag.bruddaktivitetsplikt

import no.nav.aap.komponenter.dbconnect.DBConnection
import java.time.LocalDateTime

class BruddAktivitetspliktRepository(private val connection: DBConnection) {
    fun lagreBruddAktivitetspliktHendelse(request: BruddAktivitetspliktRequest) {
        val query = """
            INSERT INTO BRUDD_AKTIVITETSPLIKT (SAKSNUMMER, BRUDD, PERIODE, BEGRUNNELSE, PARAGRAF, OPPRETTET_TID ) VALUES (?, ?, ?::daterange, ?, ?, ?)
            """.trimIndent()

        connection.executeBatch(query, request.perioder) {
            setParams { periode ->
                setString(1, request.saksnummer)
                setEnumName(2, request.brudd)
                setPeriode(3, periode)
                setString(4, request.begrunnelse)
                setEnumName(5, request.paragraf)
                setLocalDateTime(6, LocalDateTime.now())
            }
        }
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
                    begrunnelse = row.getString("BEGRUNNELSE")
                )
            }
        }
    }
}