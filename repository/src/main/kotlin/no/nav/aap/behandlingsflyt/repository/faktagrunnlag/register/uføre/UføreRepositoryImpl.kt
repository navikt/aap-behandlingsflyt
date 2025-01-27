package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.register.uføre

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.Uføre
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UføreGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.UføreRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.lookup.repository.Factory

class UføreRepositoryImpl(private val connection: DBConnection) : UføreRepository {
    companion object : Factory<UføreRepositoryImpl> {
        override fun konstruer(connection: DBConnection): UføreRepositoryImpl {
            return UføreRepositoryImpl(connection)
        }
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): UføreGrunnlag? {
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

    override fun lagre(behandlingId: BehandlingId, uføre: Uføre) {
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

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        require(fraBehandling != tilBehandling)
        connection.execute("INSERT INTO UFORE_GRUNNLAG (BEHANDLING_ID, UFORE_ID) SELECT ?, UFORE_ID FROM UFORE_GRUNNLAG WHERE AKTIV AND BEHANDLING_ID = ?") {
            setParams {
                setLong(1, tilBehandling.toLong())
                setLong(2, fraBehandling.toLong())
            }
        }
    }
}
