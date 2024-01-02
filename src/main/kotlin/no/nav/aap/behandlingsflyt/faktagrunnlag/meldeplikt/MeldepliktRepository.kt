package no.nav.aap.behandlingsflyt.faktagrunnlag.meldeplikt

import no.nav.aap.behandlingsflyt.Periode
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.meldeplikt.Fritaksvurdering
import no.nav.aap.behandlingsflyt.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection

class MeldepliktRepository(private val connection: DBConnection) {

    fun hentHvisEksisterer(behandlingId: BehandlingId): MeldepliktGrunnlag? {
        return connection.queryList(
            """
            SELECT f.ID AS MELDEPLIKT_ID, v.PERIODE, v.BEGRUNNELSE, v.HAR_FRITAK
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
                    begrunnelse = row.getString("BEGRUNNELSE"),
                    harFritak = row.getBoolean("HAR_FRITAK")
                )
            }
        }
            .grupperOgMapTilGrunnlag(behandlingId)
            .firstOrNull()
    }

    private data class MeldepliktInternal(
        val meldepliktId: Long,
        val periode: Periode,
        val begrunnelse: String,
        val harFritak: Boolean
    )

    private fun Iterable<MeldepliktInternal>.grupperOgMapTilGrunnlag(behandlingId: BehandlingId): List<MeldepliktGrunnlag> {
        return this
            .groupBy(MeldepliktInternal::meldepliktId) { meldeplikt ->
                Fritaksvurdering(
                    periode = meldeplikt.periode,
                    begrunnelse = meldeplikt.begrunnelse,
                    harFritak = meldeplikt.harFritak
                )
            }
            .map { (meldepliktId, fritaksvurderinger) ->
                MeldepliktGrunnlag(
                    id = meldepliktId,
                    behandlingId = behandlingId,
                    vurderinger = fritaksvurderinger
                )
            }
    }

    fun lagre(behandlingId: BehandlingId, vurderinger: List<Fritaksvurdering>) {
        val meldepliktGrunnlag = hentHvisEksisterer(behandlingId)

        if (meldepliktGrunnlag?.vurderinger == vurderinger) return

        if (meldepliktGrunnlag != null) {
            deaktiverEksisterende(behandlingId)
        }

        val meldepliktId = connection.executeReturnKey("INSERT INTO MELDEPLIKT_FRITAK DEFAULT VALUES")

        connection.execute("INSERT INTO MELDEPLIKT_FRITAK_GRUNNLAG (BEHANDLING_ID, MELDEPLIKT_ID) VALUES (?, ?)") {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, meldepliktId)
            }
        }

        vurderinger.forEach { vurdering ->
            connection.execute("INSERT INTO MELDEPLIKT_FRITAK_VURDERING (MELDEPLIKT_ID, PERIODE, BEGRUNNELSE, HAR_FRITAK) VALUES (?, ?::daterange, ?, ?)") {
                setParams {
                    setLong(1, meldepliktId)
                    setPeriode(2, vurdering.periode)
                    setString(3, vurdering.begrunnelse)
                    setBoolean(4, vurdering.harFritak)
                }
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
