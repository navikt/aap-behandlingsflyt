package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

class BistandRepository(private val connection: DBConnection) {

    fun hentHvisEksisterer(behandlingId: BehandlingId): BistandGrunnlag? {
        return connection.queryFirstOrNull(
            """
            SELECT g.ID, b.BEGRUNNELSE, b.ER_BEHOV_FOR_BISTAND
            FROM BISTAND_GRUNNLAG g
            INNER JOIN BISTAND b ON g.BISTAND_ID = b.ID
            WHERE g.AKTIV AND g.BEHANDLING_ID = ?
            """.trimIndent()
        ) {
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

        val bistandId =
            connection.executeReturnKey("INSERT INTO BISTAND (BEGRUNNELSE, ER_BEHOV_FOR_BISTAND) VALUES (?, ?)") {
                setParams {
                    setString(1, bistandVurdering.begrunnelse)
                    setBoolean(2, bistandVurdering.erBehovForBistand)
                }
            }

        connection.execute("INSERT INTO BISTAND_GRUNNLAG (BEHANDLING_ID, BISTAND_ID) VALUES (?, ?)") {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, bistandId)
            }
        }
    }

    private fun deaktiverEksisterende(behandlingId: BehandlingId) {
        connection.execute("UPDATE BISTAND_GRUNNLAG SET AKTIV = FALSE WHERE AKTIV AND BEHANDLING_ID = ?") {
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
        connection.execute("INSERT INTO BISTAND_GRUNNLAG (BEHANDLING_ID, BISTAND_ID) SELECT ?, BISTAND_ID FROM BISTAND_GRUNNLAG WHERE AKTIV AND BEHANDLING_ID = ?") {
            setParams {
                setLong(1, tilBehandling.toLong())
                setLong(2, fraBehandling.toLong())
            }
        }
    }
}
