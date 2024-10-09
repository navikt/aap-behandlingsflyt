package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.verdityper.Prosent
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import java.time.LocalDate
import java.time.LocalDateTime

class ArbeidsevneRepository(private val connection: DBConnection) {
    
    fun hentHvisEksisterer(behandlingId: BehandlingId): ArbeidsevneGrunnlag? {
        return connection.queryList(
            """
            SELECT a.ID AS ARBEIDSEVNE_ID, v.BEGRUNNELSE, v.FRA_DATO, v.ARBEIDSEVNE, v.OPPRETTET_TID
            FROM ARBEIDSEVNE_GRUNNLAG g
            INNER JOIN ARBEIDSEVNE a ON g.ARBEIDSEVNE_ID = a.ID
            INNER JOIN ARBEIDSEVNE_VURDERING v ON a.ID = v.ARBEIDSEVNE_ID
            WHERE g.AKTIV AND g.BEHANDLING_ID = ?
            """.trimIndent()
        ) {
            setParams { setLong(1, behandlingId.toLong()) }
            setRowMapper { row ->
                ArbeidsevneInternal(
                    row.getLong("ARBEIDSEVNE_ID"),
                    row.getString("BEGRUNNELSE"),
                    row.getLocalDate("FRA_DATO"),
                    Prosent(row.getInt("ARBEIDSEVNE")),
                    row.getLocalDateTime("OPPRETTET_TID")
                )
            }
        }.toGrunnlag(behandlingId)
    }

    private data class ArbeidsevneInternal(
        val arbeidsevneId: Long,
        val begrunnelse: String,
        val fraDato: LocalDate,
        val arbeidsevne: Prosent,
        val opprettetTid: LocalDateTime
    ) {
        fun toArbeidsevnevurdering(): Arbeidsevnevurdering {
            return Arbeidsevnevurdering(begrunnelse, arbeidsevne, fraDato, opprettetTid)
        }
    }

    private fun List<ArbeidsevneInternal>.toGrunnlag(behandlingId: BehandlingId): ArbeidsevneGrunnlag? {
        return groupBy(ArbeidsevneInternal::arbeidsevneId, ArbeidsevneInternal::toArbeidsevnevurdering)
            .map { (arbeidsevneId, arbeidsevnevurderinger) -> ArbeidsevneGrunnlag(arbeidsevneId, behandlingId, arbeidsevnevurderinger) }
            .takeIf { it.isNotEmpty() }?.single()
    }

    fun lagre(behandlingId: BehandlingId, arbeidsevnevurderinger: List<Arbeidsevnevurdering>) {
        val eksisterendeArbeidsevneGrunnlag = hentHvisEksisterer(behandlingId)

        if (eksisterendeArbeidsevneGrunnlag?.vurderinger == arbeidsevnevurderinger) return

        if (eksisterendeArbeidsevneGrunnlag != null) deaktiverEksisterende(behandlingId)

        val arbeidsevneId = connection.executeReturnKey("INSERT INTO ARBEIDSEVNE DEFAULT VALUES")

        connection.execute("INSERT INTO ARBEIDSEVNE_GRUNNLAG (BEHANDLING_ID, ARBEIDSEVNE_ID) VALUES (?, ?)") {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, arbeidsevneId)
            }
        }

        arbeidsevnevurderinger.lagre(arbeidsevneId)
    }

    private fun List<Arbeidsevnevurdering>.lagre(arbeidsevneId: Long) {
        connection.executeBatch(
            """
            INSERT INTO ARBEIDSEVNE_VURDERING 
            (ARBEIDSEVNE_ID, FRA_DATO, BEGRUNNELSE, ARBEIDSEVNE, OPPRETTET_TID) VALUES (?, ?, ?, ?, ?)
            """.trimIndent(),
            this
        ) {
            setParams {
                setLong(1, arbeidsevneId)
                setLocalDate(2, it.fraDato)
                setString(3, it.begrunnelse)
                setInt(4, it.arbeidsevne.prosentverdi())
                setLocalDateTime(5, it.opprettetTid)
            }
        }
    }

    private fun deaktiverEksisterende(behandlingId: BehandlingId) {
        connection.execute("UPDATE ARBEIDSEVNE_GRUNNLAG SET AKTIV = FALSE WHERE AKTIV AND BEHANDLING_ID = ?") {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setResultValidator { rowsUpdated ->
                require(rowsUpdated == 1)
            }
        }
    }

    fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        require(fraBehandling != tilBehandling)
        connection.execute("INSERT INTO ARBEIDSEVNE_GRUNNLAG (BEHANDLING_ID, ARBEIDSEVNE_ID) SELECT ?, ARBEIDSEVNE_ID FROM ARBEIDSEVNE_GRUNNLAG WHERE AKTIV AND BEHANDLING_ID = ?") {
            setParams {
                setLong(1, tilBehandling.toLong())
                setLong(2, fraBehandling.toLong())
            }
        }
    }
}
