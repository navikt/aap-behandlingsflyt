package no.nav.aap.behandlingsflyt.faktagrunnlag.meldeplikt

import no.nav.aap.behandlingsflyt.avklaringsbehov.meldeplikt.Fritaksvurdering
import no.nav.aap.behandlingsflyt.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection

class MeldepliktRepository(private val connection: DBConnection) {

    fun hentHvisEksisterer(behandlingId: BehandlingId): MeldepliktGrunnlag? {
        return connection.queryFirstOrNull("SELECT ID FROM MELDEPLIKT_FRITAK_GRUNNLAG WHERE AKTIV AND BEHANDLING_ID = ?") {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper { row ->
                val id = row.getLong("ID")
                MeldepliktGrunnlag(
                    id = id,
                    behandlingId = behandlingId,
                    vurderinger = hentFritaksvurderinger(id)
                )
            }
        }
    }

    private fun hentFritaksvurderinger(id: Long): List<Fritaksvurdering> {
        return connection.queryList("SELECT PERIODE, BEGRUNNELSE, HAR_FRITAK FROM MELDEPLIKT_FRITAK_VURDERING WHERE GRUNNLAG_ID = ?") {
            setParams {
                setLong(1, id)
            }
            setRowMapper { row ->
                Fritaksvurdering(
                    periode = row.getPeriode("PERIODE"),
                    begrunnelse = row.getString("BEGRUNNELSE"),
                    harFritak = row.getBoolean("HAR_FRITAK")
                )
            }
        }
    }

    fun lagre(behandlingId: BehandlingId, vurderinger: List<Fritaksvurdering>) {
        val meldepliktGrunnlag = hentHvisEksisterer(behandlingId)

        if (meldepliktGrunnlag?.vurderinger == vurderinger) return

        if (meldepliktGrunnlag != null) {
            deaktiverEksisterende(behandlingId)
        }

        val id = connection.executeReturnKey("INSERT INTO MELDEPLIKT_FRITAK_GRUNNLAG (BEHANDLING_ID) VALUES (?)") {
            setParams {
                setLong(1, behandlingId.toLong())
            }
        }

        vurderinger.forEach { vurdering ->
            connection.execute("INSERT INTO MELDEPLIKT_FRITAK_VURDERING (GRUNNLAG_ID, PERIODE, BEGRUNNELSE, HAR_FRITAK) VALUES (?, ?::daterange, ?, ?)") {
                setParams {
                    setLong(1, id)
                    setPeriode(2, vurdering.periode)
                    setString(3, vurdering.begrunnelse)
                    setBoolean(4, vurdering.harFritak)
                }
            }
        }
    }

    private fun deaktiverEksisterende(behandlingId: BehandlingId) {
        connection.execute("UPDATE MELDEPLIKT_FRITAK_GRUNNLAG SET AKTIV = 'FALSE' WHERE AKTIV AND BEHANDLING_ID = ?") {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setResultValidator { rowsUpdated ->
                require(rowsUpdated == 1)
            }
        }
    }

    fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        val fraId =
            connection.queryFirstOrNull("SELECT ID FROM MELDEPLIKT_FRITAK_GRUNNLAG WHERE AKTIV AND BEHANDLING_ID = ?") {
                setParams {
                    setLong(1, fraBehandling.toLong())
                }
                setRowMapper { row ->
                    row.getLong("ID")
                }
            }

        if (fraId == null) {
            return
        }

        val tilId = connection.executeReturnKey("INSERT INTO MELDEPLIKT_FRITAK_GRUNNLAG (BEHANDLING_ID) VALUES (?)") {
            setParams {
                setLong(1, tilBehandling.toLong())
            }
        }

        connection.execute("INSERT INTO MELDEPLIKT_FRITAK_VURDERING (GRUNNLAG_ID, PERIODE, BEGRUNNELSE, HAR_FRITAK) SELECT ?, PERIODE, BEGRUNNELSE, HAR_FRITAK FROM MELDEPLIKT_FRITAK_VURDERING WHERE GRUNNLAG_ID = ?") {
            setParams {
                setLong(1, tilId)
                setLong(2, fraId)
            }
        }
    }
}
