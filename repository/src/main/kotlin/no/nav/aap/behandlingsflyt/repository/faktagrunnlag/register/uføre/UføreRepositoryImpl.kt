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
            SELECT *
            FROM UFORE_GRUNNLAG
            WHERE AKTIV AND BEHANDLING_ID = ?
            """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper { row ->
                UføreGrunnlag(
                    id = row.getLong("id"),
                    behandlingId = behandlingId,
                    vurderinger = hentVurderinger(row.getLong("ufore_id"))
                )
            }
        }
    }

    override fun hentEldsteGrunnlag(behandlingId: BehandlingId): UføreGrunnlag? {
        return connection.queryFirstOrNull(
            """
            SELECT *
            FROM UFORE_GRUNNLAG
            WHERE BEHANDLING_ID = ?
            ORDER BY OPPRETTET_TID
            LIMIT 1
            """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper { row ->
                UføreGrunnlag(
                    id = row.getLong("id"),
                    behandlingId = behandlingId,
                    vurderinger = hentVurderinger(row.getLong("ufore_id"))
                )
            }
        }
    }

    override fun slett(behandlingId: BehandlingId) {

        val uforeIds = getUforeIds(behandlingId)

        connection.execute("""
           
            delete from UFORE_GRADERING where id = ANY(?::bigint[]);
            delete from UFORE where behandling_id = ?;
            delete from UFORE_GRUNNLAG where behandling_id = ?;
           
        """.trimIndent()) {
            setParams {
                setLong(1, behandlingId.id)
                setLongArray(2, uforeIds)
                setLong(3, behandlingId.id)

            }
        }
    }

    private fun getUforeIds(behandlingId: BehandlingId): List<Long> = connection.queryList(
        """
                    SELECT ufore_id
                    FROM UFORE_GRUNNLAG
                    WHERE behandling_id = ?
                 
                """.trimIndent()
    ) {
        setParams { setLong(1, behandlingId.id) }
        setRowMapper { row ->
            row.getLong("ufore_id")
        }
    }

    private fun hentVurderinger(uføreId: Long): List<Uføre> {
        return connection.queryList(
            """
            SELECT * FROM UFORE_GRADERING WHERE UFORE_ID = ?
        """.trimIndent()
        ) {
            setParams {
                setLong(1, uføreId)
            }
            setRowMapper { row ->
                Uføre(
                    virkningstidspunkt = row.getLocalDate("virkningstidspunkt"),
                    uføregrad = Prosent(row.getInt("uforegrad"))
                )
            }
        }
    }

    override fun lagre(behandlingId: BehandlingId, uføre: List<Uføre>) {
        val eksisterendeUføreGrunnlag = hentHvisEksisterer(behandlingId)

        if (eksisterendeUføreGrunnlag?.vurderinger == uføre) return

        if (eksisterendeUføreGrunnlag != null) {
            deaktiverEksisterende(behandlingId)
        }

        val uføreId = connection.executeReturnKey("INSERT INTO UFORE DEFAULT VALUES")

        connection.execute("INSERT INTO UFORE_GRUNNLAG (BEHANDLING_ID, UFORE_ID) VALUES (?, ?)") {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, uføreId)
            }
        }

        connection.executeBatch(
            "INSERT INTO UFORE_GRADERING (UFORE_ID, UFOREGRAD, VIRKNINGSTIDSPUNKT) VALUES (?, ?, ?)",
            uføre
        ) {
            setParams {
                setLong(1, uføreId)
                setInt(2, it.uføregrad.prosentverdi())
                setLocalDate(3, it.virkningstidspunkt)
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
