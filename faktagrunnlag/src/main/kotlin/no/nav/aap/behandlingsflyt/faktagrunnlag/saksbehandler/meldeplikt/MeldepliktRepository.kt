package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import java.time.LocalDate
import java.time.LocalDateTime

class MeldepliktRepository(private val connection: DBConnection) {

    companion object {
        private val FRITAK_QUERY = """
            SELECT f.ID AS MELDEPLIKT_ID, v.HAR_FRITAK, v.OPPRINNELIG_FRA_DATO, v.GJELDENDE_PERIODE, v.BEGRUNNELSE, v.OPPRETTET_TID
            FROM MELDEPLIKT_FRITAK_GRUNNLAG g
            INNER JOIN MELDEPLIKT_FRITAK f ON g.MELDEPLIKT_ID = f.ID
            INNER JOIN MELDEPLIKT_FRITAK_VURDERING v ON f.ID = v.MELDEPLIKT_ID
            WHERE g.AKTIV AND g.BEHANDLING_ID = ?
            """.trimIndent()

        private val INSERT_FRITAKSVURDERING_QUERY = """
            INSERT INTO MELDEPLIKT_FRITAK_VURDERING 
            (MELDEPLIKT_ID, BEGRUNNELSE, HAR_FRITAK, OPPRINNELIG_FRA_DATO, GJELDENDE_PERIODE, OPPRETTET_TID) VALUES (?, ?, ?, ?, ?::daterange, ?)
            """.trimIndent()
    }

    fun hentHvisEksisterer(behandlingId: BehandlingId): MeldepliktGrunnlag? {
        return connection.queryList(FRITAK_QUERY) {
            setParams { setLong(1, behandlingId.toLong()) }
            setRowMapper { row ->
                MeldepliktInternal(
                    meldepliktId = row.getLong("MELDEPLIKT_ID"),
                    harFritak = row.getBoolean("HAR_FRITAK"),
                    opprinneligFraDato = row.getLocalDate("OPPRINNELIG_FRA_DATO"),
                    gjeldendePeriode = row.getPeriode("GJELDENDE_PERIODE"),
                    begrunnelse = row.getString("BEGRUNNELSE"),
                    vurderingOpprettet = row.getLocalDateTime("OPPRETTET_TID"),
                )
            }
        }.grupperOgMapTilGrunnlag(behandlingId).firstOrNull()
    }

    private data class MeldepliktInternal(
        val meldepliktId: Long,
        val harFritak: Boolean,
        val opprinneligFraDato: LocalDate,
        val gjeldendePeriode: Periode,
        val begrunnelse: String,
        val vurderingOpprettet: LocalDateTime
    )

    private fun Iterable<MeldepliktInternal>.grupperOgMapTilGrunnlag(behandlingId: BehandlingId): List<MeldepliktGrunnlag> {
        return groupBy(MeldepliktInternal::meldepliktId) { it.toFritaksvurdering() }
            .map { (meldepliktId, fritaksvurderinger) -> MeldepliktGrunnlag(meldepliktId, behandlingId, fritaksvurderinger) }
    }


    private fun MeldepliktInternal.toFritaksvurdering(): Fritaksvurdering {
        return Fritaksvurdering(harFritak, opprinneligFraDato, gjeldendePeriode, begrunnelse, vurderingOpprettet)
    }

    fun lagre(behandlingId: BehandlingId, vurderinger: List<Fritaksvurdering>) {
        val meldepliktGrunnlag = hentHvisEksisterer(behandlingId)

        if (meldepliktGrunnlag?.vurderinger == vurderinger) return

        if (meldepliktGrunnlag != null) deaktiverEksisterende(behandlingId)

        val meldepliktId = connection.executeReturnKey("INSERT INTO MELDEPLIKT_FRITAK DEFAULT VALUES")

        connection.execute("INSERT INTO MELDEPLIKT_FRITAK_GRUNNLAG (BEHANDLING_ID, MELDEPLIKT_ID) VALUES (?, ?)") {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, meldepliktId)
            }
        }

        vurderinger.lagre(meldepliktId)
    }

    private fun List<Fritaksvurdering>.lagre(meldepliktId: Long) {
        connection.executeBatch(
            INSERT_FRITAKSVURDERING_QUERY,
            this
        ) {
            setParams {
                setLong(1, meldepliktId)
                setString(2, it.begrunnelse)
                setBoolean(3, it.harFritak)
                setLocalDate(4, it.opprinneligFraDato)
                setPeriode(5, it.periode)
                setLocalDateTime(6, it.opprettetTid)
            }
        }
    }

    private fun deaktiverEksisterende(behandlingId: BehandlingId) {
        connection.execute("UPDATE MELDEPLIKT_FRITAK_GRUNNLAG SET AKTIV = FALSE WHERE AKTIV AND BEHANDLING_ID = ?") {
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
        connection.execute("INSERT INTO MELDEPLIKT_FRITAK_GRUNNLAG (BEHANDLING_ID, MELDEPLIKT_ID) SELECT ?, MELDEPLIKT_ID FROM MELDEPLIKT_FRITAK_GRUNNLAG WHERE AKTIV AND BEHANDLING_ID = ?") {
            setParams {
                setLong(1, tilBehandling.toLong())
                setLong(2, fraBehandling.toLong())
            }
        }
    }
}
