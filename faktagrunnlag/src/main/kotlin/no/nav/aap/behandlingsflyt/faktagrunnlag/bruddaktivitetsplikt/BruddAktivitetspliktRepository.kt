package no.nav.aap.behandlingsflyt.faktagrunnlag.bruddaktivitetsplikt

import no.nav.aap.komponenter.dbconnect.DBConnection
import java.time.LocalDateTime

class BruddAktivitetspliktRepository(private val connection: DBConnection) {
    fun lagreBruddAktivitetspliktHendelse(request: BruddAktivitetspliktRequest) {
        connection.execute(
        """
            INSERT INTO BRUDD_AKTIVITETSPLIKT (SAK_ID, BRUDD, PERIODE, BEGRUNNELSE, PARAGRAF, OPPRETTET_TID ) VALUES (?, ?, ?::daterange, ?, ?, ?)
        """.trimIndent()) {
            setParams {
                setLong(1, request.sakId)
                setEnumName(2, request.brudd)
                setPeriode(3, request.periode)
                setString(4, request.begrunnelse)
                setString(5, request.paragraf)
                setLocalDateTime(6, LocalDateTime.now())
            }
        }
    }

    fun hentBruddAktivitetspliktHendelser(saksnummer: String) : List<BruddAktivitetspliktHendelse> {
        return connection.queryList(
            """
                SELECT *
                FROM BRUDD_AKTIVITETSPLIKT
                WHERE SAKSNUMMER = ?
            """.trimIndent()
        ) {
            setParams { setString(1, saksnummer) }
            setRowMapper { row ->
                BruddAktivitetspliktHendelse(
                    brudd = row.getEnum("BRUDD"),
                    paragraf = row.getString("PARAGRAF"),
                    periode = row.getPeriode("PERIODE")
                )
            }
        }
    }
}