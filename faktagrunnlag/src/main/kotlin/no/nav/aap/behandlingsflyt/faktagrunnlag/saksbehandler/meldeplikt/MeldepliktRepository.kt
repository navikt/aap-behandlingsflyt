package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

class MeldepliktRepository(private val connection: DBConnection) {

    fun hentHvisEksisterer(behandlingId: BehandlingId): MeldepliktGrunnlag? {
        return connection.queryList(
            """
            SELECT f.ID AS MELDEPLIKT_ID, v.PERIODE, v.HAR_FRITAK
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
                    periode = row.getPeriode("PERIODE"),
                    harFritak = row.getBoolean("HAR_FRITAK")
                )
            }
        }
            .grupperOgMapTilGrunnlag(behandlingId)
            .firstOrNull()
    }

    private fun meldepliktGrunnlag(behandlingId: BehandlingId, meldepliktId: Long, fritaksPerioder: List<FritaksPeriode>) = connection.queryFirstOrNull("""
        SELECT BEGRUNNELSE, OPPRETTET_TID FROM MELDEPLIKT_FRITAK_GRUNNLAG WHERE AKTIV AND BEHANDLING_ID = ?
    """.trimIndent()
    ) {
        setParams { setLong(1, behandlingId.toLong()) }
        setRowMapper { row ->
            MeldepliktGrunnlag(
                meldepliktId,
                behandlingId,
                row.getLocalDateTime("OPPRETTET_TID"),
                Fritaksvurdering(fritaksPerioder, row.getString("BEGRUNNELSE"))
            )
        }
    }

    private data class MeldepliktInternal(
        val meldepliktId: Long,
        val periode: Periode,
        val harFritak: Boolean
    ) {
        fun fritaksPeriode() = FritaksPeriode(periode, harFritak)
    }

    private fun Iterable<MeldepliktInternal>.grupperOgMapTilGrunnlag(behandlingId: BehandlingId) =
        groupBy(MeldepliktInternal::meldepliktId, MeldepliktInternal::fritaksPeriode)
            .map { (meldepliktId, fritaksPerioder) -> meldepliktGrunnlag(behandlingId, meldepliktId, fritaksPerioder) }

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

        vurdering.fritaksPerioder.forEach { lagreFritaksPeriode(meldepliktId, it) }
    }

    private fun lagreFritaksPeriode(meldepliktId: Long, fritaksPeriode: FritaksPeriode) {
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
        connection.execute("INSERT INTO MELDEPLIKT_FRITAK_GRUNNLAG (BEHANDLING_ID, MELDEPLIKT_ID) SELECT ?, MELDEPLIKT_ID FROM MELDEPLIKT_FRITAK_GRUNNLAG WHERE AKTIV AND BEHANDLING_ID = ?") {
            setParams {
                setLong(1, tilBehandling.toLong())
                setLong(2, fraBehandling.toLong())
            }
        }
    }
}
