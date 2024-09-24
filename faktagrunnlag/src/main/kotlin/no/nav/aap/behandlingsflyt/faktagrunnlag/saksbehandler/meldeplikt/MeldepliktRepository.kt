package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import java.time.LocalDateTime

class MeldepliktRepository(private val connection: DBConnection) {

    companion object {
        private const val FRITAKSPERIODE_QUERY =
            "SELECT p.PERIODE, p.BEGRUNNELSE FROM MELDEPLIKT_FRITAK_PERIODE p WHERE p.VURDERING_ID = ?"

        private val FRITAK_QUERY = """
            SELECT f.ID AS MELDEPLIKT_ID, v.ID AS VURDERING_ID, v.BEGRUNNELSE, v.OPPRETTET_TID
            FROM MELDEPLIKT_FRITAK_GRUNNLAG g
            INNER JOIN MELDEPLIKT_FRITAK f ON g.MELDEPLIKT_ID = f.ID
            INNER JOIN MELDEPLIKT_FRITAK_VURDERING v ON f.ID = v.MELDEPLIKT_ID
            WHERE g.AKTIV AND g.BEHANDLING_ID = ?
            """.trimIndent()
    }

    fun hentHvisEksisterer(behandlingId: BehandlingId): MeldepliktGrunnlag? {
        return connection.queryList(FRITAK_QUERY) {
            setParams { setLong(1, behandlingId.toLong()) }
            setRowMapper { row ->
                MeldepliktInternal(
                    meldepliktId = row.getLong("MELDEPLIKT_ID"),
                    vurderingId = row.getLong("VURDERING_ID"),
                    begrunnelse = row.getString("BEGRUNNELSE"),
                    vurderingOpprettet = row.getLocalDateTime("OPPRETTET_TID"),
                )
            }
        }.grupperOgMapTilGrunnlag(behandlingId).firstOrNull()
    }

    private data class MeldepliktInternal(
        val meldepliktId: Long,
        val vurderingId: Long,
        val begrunnelse: String,
        val vurderingOpprettet: LocalDateTime
    )

    private fun Iterable<MeldepliktInternal>.grupperOgMapTilGrunnlag(behandlingId: BehandlingId): List<MeldepliktGrunnlag> {
        return groupBy(MeldepliktInternal::meldepliktId) { it.toFritaksvurdering() }
            .map { (meldepliktId, fritaksvurderinger) -> MeldepliktGrunnlag(meldepliktId, behandlingId, fritaksvurderinger) }
    }


    private fun MeldepliktInternal.toFritaksvurdering(): Fritaksvurdering {
        return Fritaksvurdering(fritaksperioder(this.vurderingId), this.begrunnelse, vurderingOpprettet)
    }

    private fun fritaksperioder(vurderingId: Long): List<Fritaksperiode> {
        return connection.queryList(FRITAKSPERIODE_QUERY) {
            setParams { setLong(1, vurderingId) }
            setRowMapper { row ->
                Fritaksperiode(
                    row.getPeriode("PERIODE"),
                    row.getBoolean("HAR_FRITAK")
                )
            }
        }
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
        vurderinger.forEach { lagreFritaksvurdering(meldepliktId, it) }
    }

    private fun lagreFritaksvurdering(meldepliktId: Long, vurdering: Fritaksvurdering) {
        val vurderingId = connection.executeReturnKey(
            "INSERT INTO MELDEPLIKT_FRITAK_VURDERING (MELDEPLIKT_ID, BEGRUNNELSE, OPPRETTET_TID) VALUES (?, ?, ?)"
        ) {
            setParams {
                setLong(1, meldepliktId)
                setString(2, vurdering.begrunnelse)
                setLocalDateTime(3, vurdering.opprettetTid)
            }
        }
        vurdering.fritaksperioder.lagre(vurderingId)
    }

    private fun List<Fritaksperiode>.lagre(vurderingId: Long) {
        connection.executeBatch(
            "INSERT INTO MELDEPLIKT_FRITAK_PERIODE (VURDERING_ID, PERIODE, HAR_FRITAK) VALUES (?, ?::daterange, ?)",
            this
        ) {
            setParams {
                setLong(1, vurderingId)
                setPeriode(2, it.periode)
                setBoolean(3, it.harFritak)
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
