package no.nav.aap.behandlingsflyt.faktagrunnlag.uføre

import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.verdityper.Prosent

class UføreRepository(private val connection: DBConnection) {

    fun hentHvisEksisterer(behandlingId: BehandlingId): UføreGrunnlag? {
        return connection.queryFirstOrNull(
            """
            SELECT g.ID, u.UFOREGRAD
            FROM UFORE_GRUNNLAG g
            INNER JOIN UFORE u ON g.UFORE_ID = u.ID
            WHERE g.AKTIV AND g.BEHANDLING_ID = ?
            """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper { row ->
                UføreGrunnlag(
                    id = row.getLong("ID"),
                    behandlingId = behandlingId,
                    vurdering = Uføre(
                        uføregrad = Prosent(row.getInt("UFOREGRAD"))
                    )
                )
            }
        }
    }

    fun lagre(behandlingId: BehandlingId, uføre: Uføre) {
        val eksisterendeUføreGrunnlag = hentHvisEksisterer(behandlingId)

        if (eksisterendeUføreGrunnlag?.vurdering == uføre) return

        if (eksisterendeUføreGrunnlag != null) {
            deaktiverEksisterende(behandlingId)
        }

        val uføreId =
            connection.executeReturnKey("INSERT INTO UFORE (UFOREGRAD) VALUES (?)") {
                setParams {
                    setInt(1, uføre.uføregrad.prosentverdi())
                }
            }

        connection.execute("INSERT INTO UFORE_GRUNNLAG (BEHANDLING_ID, UFORE_ID) VALUES (?, ?)") {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, uføreId)
            }
        }
    }

    private fun deaktiverEksisterende(behandlingId: BehandlingId) {
        connection.execute("UPDATE UFORE_GRUNNLAG SET AKTIV = FALSE WHERE AKTIV AND BEHANDLING_ID = ?") {
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
        connection.execute("INSERT INTO UFORE_GRUNNLAG (BEHANDLING_ID, UFORE_ID) SELECT ?, UFORE_ID FROM UFORE_GRUNNLAG WHERE AKTIV AND BEHANDLING_ID = ?") {
            setParams {
                setLong(1, tilBehandling.toLong())
                setLong(2, fraBehandling.toLong())
            }
        }
    }
}
