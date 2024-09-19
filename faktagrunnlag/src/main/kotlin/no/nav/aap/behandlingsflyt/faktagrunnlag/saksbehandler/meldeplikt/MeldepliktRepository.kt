package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

class MeldepliktRepository(private val connection: DBConnection) {

    companion object {
        private val fritaksperiodeQuery =
            "SELECT p.PERIODE, p.BEGRUNNELSE FROM MELDEPLIKT_FRITAK_PERIODE p WHERE p.VURDERING_ID = ?"
    }

    fun hentHvisEksisterer(behandlingId: BehandlingId): MeldepliktGrunnlag? {
        return connection.queryList(
            """
            SELECT f.ID AS MELDEPLIKT_ID, v.ID AS VURDERING_ID, v.BEGRUNNELSE
            FROM MELDEPLIKT_FRITAK_GRUNNLAG g
            INNER JOIN MELDEPLIKT_FRITAK f ON g.MELDEPLIKT_ID = f.ID
            INNER JOIN MELDEPLIKT_FRITAK_VURDERING v ON f.ID = v.MELDEPLIKT_ID
            WHERE g.AKTIV AND g.BEHANDLING_ID = ?
            """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper { row ->
                MeldepliktInternal(
                    meldepliktId = row.getLong("MELDEPLIKT_ID"),
                    vurderingId = row.getLong("VURDERING_ID"),
                    begrunnelse = row.getString("BEGRUNNELSE"),
                )
            }
        }.grupperOgMapTilGrunnlag(behandlingId).firstOrNull()
    }

    private data class MeldepliktInternal(
        val meldepliktId: Long,
        val vurderingId: Long,
        val begrunnelse: String,
    )

    private fun Iterable<MeldepliktInternal>.grupperOgMapTilGrunnlag(behandlingId: BehandlingId): List<MeldepliktGrunnlag> {
        return groupBy(MeldepliktInternal::meldepliktId) { it.toFritaksvurdering() }
            .map { (meldepliktId, fritaksvurderinger) -> MeldepliktGrunnlag(meldepliktId, behandlingId, fritaksvurderinger) }
    }


    private fun MeldepliktInternal.toFritaksvurdering(): Fritaksvurdering {
        return Fritaksvurdering(fritaksperioder(this.vurderingId), this.begrunnelse)
    }

    private fun fritaksperioder(vurderingId: Long): List<Fritaksperiode> {
        return connection.queryList(fritaksperiodeQuery) {
            setParams { setLong(1, vurderingId) }
            setRowMapper { row ->
                Fritaksperiode(
                    row.getPeriode("PERIODE"),
                    row.getBoolean("HAR_FRITAK")
                )
            }
        }
    }



    fun lagre(behandlingId: BehandlingId, vurdering: Fritaksvurdering) {
        val meldepliktGrunnlag = hentHvisEksisterer(behandlingId)

        if (meldepliktGrunnlag?.vurdering == vurdering) return

        if (meldepliktGrunnlag != null) deaktiverEksisterende(behandlingId)

        val meldepliktId = connection.executeReturnKey("INSERT INTO MELDEPLIKT_FRITAK DEFAULT VALUES")

        connection.execute("INSERT INTO MELDEPLIKT_FRITAK_GRUNNLAG (BEHANDLING_ID, MELDEPLIKT_ID, BEGRUNNELSE) VALUES (?, ?, ?)") {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, meldepliktId)
                setString(3, vurdering.begrunnelse)
            }
        }

        vurdering.fritaksperioder.forEach { lagreFritaksPeriode(meldepliktId, it) }
    }

    private fun lagreFritaksPeriode(meldepliktId: Long, fritaksPeriode: Fritaksperiode) {
        connection.execute("INSERT INTO MELDEPLIKT_FRITAK_VURDERING (MELDEPLIKT_ID, PERIODE, HAR_FRITAK) VALUES (?, ?::daterange, ?)") {
            setParams {
                setLong(1, meldepliktId)
                setPeriode(2, fritaksPeriode.periode)
                setBoolean(3, fritaksPeriode.harFritak)
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
        connection.execute("INSERT INTO MELDEPLIKT_FRITAK_GRUNNLAG (BEHANDLING_ID, MELDEPLIKT_ID, BEGRUNNELSE) SELECT ?, MELDEPLIKT_ID, BEGRUNNELSE FROM MELDEPLIKT_FRITAK_GRUNNLAG WHERE AKTIV AND BEHANDLING_ID = ?") {
            setParams {
                setLong(1, tilBehandling.toLong())
                setLong(2, fraBehandling.toLong())
            }
        }
    }
}
