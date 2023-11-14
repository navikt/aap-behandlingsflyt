package no.nav.aap.behandlingsflyt.faktagrunnlag.bistand

import no.nav.aap.behandlingsflyt.avklaringsbehov.bistand.BistandVurdering
import no.nav.aap.behandlingsflyt.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection

class BistandRepository(private val connection: DBConnection) {

    fun hentHvisEksisterer(behandlingId: BehandlingId): BistandGrunnlag? {
        return connection.queryFirstOrNull("SELECT ID, BEGRUNNELSE, ER_BEHOV_FOR_BISTAND FROM BISTAND_GRUNNLAG WHERE AKTIV AND BEHANDLING_ID = ?") {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper { row ->
                BistandGrunnlag(
                    id = row.getLong("ID"),
                    behandlingId = behandlingId,
                    vurdering = BistandVurdering(
                        begrunnelse = row.getString("BEGRUNNELSE"),
                        erBehovForBistand = row.getBoolean("ER_BEHOV_FOR_BISTAND")
                    )
                )
            }
        }
    }

    fun lagre(behandlingId: BehandlingId, bistandVurdering: BistandVurdering) {
        val eksisterendeBistandGrunnlag = hentHvisEksisterer(behandlingId)

        if (eksisterendeBistandGrunnlag?.vurdering == bistandVurdering) return

        if (eksisterendeBistandGrunnlag != null) {
            deaktiverEksisterende(behandlingId)
        }

        connection.execute("INSERT INTO BISTAND_GRUNNLAG (BEHANDLING_ID, BEGRUNNELSE, ER_BEHOV_FOR_BISTAND) VALUES (?, ?, ?)") {
            setParams {
                setLong(1, behandlingId.toLong())
                setString(2, bistandVurdering.begrunnelse)
                setBoolean(3, bistandVurdering.erBehovForBistand)
            }
        }
    }

    private fun deaktiverEksisterende(behandlingId: BehandlingId) {
        connection.execute("UPDATE BISTAND_GRUNNLAG SET AKTIV = 'FALSE' WHERE AKTIV AND BEHANDLING_ID = ?") {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setResultValidator { rowsUpdated ->
                require(rowsUpdated == 1)
            }
        }
    }

    fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        connection.execute("INSERT INTO BISTAND_GRUNNLAG (BEHANDLING_ID, BEGRUNNELSE, ER_BEHOV_FOR_BISTAND) SELECT ?, BEGRUNNELSE, ER_BEHOV_FOR_BISTAND FROM BISTAND_GRUNNLAG WHERE AKTIV AND BEHANDLING_ID = ?") {
            setParams {
                setLong(1, tilBehandling.toLong())
                setLong(2, fraBehandling.toLong())
            }
        }
    }
}
