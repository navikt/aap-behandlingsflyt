package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.klage

import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.påklagetbehandling.PåklagetBehandlingGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.påklagetbehandling.PåklagetBehandlingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.påklagetbehandling.PåklagetBehandlingVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.lookup.repository.Factory

class PåklagetBehandlingRepositoryImpl(private val connection: DBConnection) : PåklagetBehandlingRepository {

    companion object : Factory<PåklagetBehandlingRepositoryImpl> {
        override fun konstruer(connection: DBConnection): PåklagetBehandlingRepositoryImpl {
            return PåklagetBehandlingRepositoryImpl(connection)
        }
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): PåklagetBehandlingGrunnlag? {
        val query = """
            SELECT * FROM PAAKLAGET_BEHANDLING_VURDERING
            WHERE id IN (
                SELECT vurdering_id FROM PAAKLAGET_BEHANDLING_GRUNNLAG
                WHERE BEHANDLING_ID = ? AND AKTIV = TRUE
            )
        """.trimIndent()
        return connection.queryFirstOrNull(query) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper(::mapGrunnlag)
        }
    }

    override fun lagre(behandlingId: BehandlingId, påklagetBehandlingVurdering: PåklagetBehandlingVurdering) {
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)
        val nyttGrunnlag = PåklagetBehandlingGrunnlag(vurdering = påklagetBehandlingVurdering)
        if (eksisterendeGrunnlag != nyttGrunnlag) {
            eksisterendeGrunnlag?.let {
                deaktiverEksisterende(behandlingId)
            }
            lagre(behandlingId, nyttGrunnlag)
        }
    }

    private fun lagre(behandlingId: BehandlingId, nyttGrunnlag: PåklagetBehandlingGrunnlag) {
        val vurderingId = lagreVurdering(nyttGrunnlag.vurdering)
        val query = """
            INSERT INTO PAAKLAGET_BEHANDLING_GRUNNLAG (BEHANDLING_ID, VURDERING_ID, AKTIV) 
            VALUES (?, ?, TRUE)
        """.trimIndent()

        connection.execute(query) {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, vurderingId)
            }
        }
    }

    private fun lagreVurdering(vurdering: PåklagetBehandlingVurdering): Long {
        val query = """
            INSERT INTO PAAKLAGET_BEHANDLING_VURDERING 
            (TYPE_VEDTAK, PAAKLAGET_BEHANDLING_ID, VURDERT_AV) 
            VALUES (?, ?, ?)
        """.trimIndent()

        return connection.executeReturnKey(query) {
            setParams {
                setEnumName(1, vurdering.påklagetVedtakType)
                setLong(2, vurdering.påklagetBehandling?.id)
                setString(3, vurdering.vurdertAv)
            }
            setResultValidator { rowsUpdated ->
                require(rowsUpdated == 1)
            }
        }
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        // Skal ikke kopieres
    }

    private fun mapGrunnlag(row: Row): PåklagetBehandlingGrunnlag {
        return PåklagetBehandlingGrunnlag(
            vurdering = mapPåklagetBehandlingVurdering(row),
        )
    }

    private fun mapPåklagetBehandlingVurdering(row: Row): PåklagetBehandlingVurdering {
        return PåklagetBehandlingVurdering(
            påklagetVedtakType = row.getEnum("type_vedtak"),
            påklagetBehandling = row.getLongOrNull("PAAKLAGET_BEHANDLING_ID")?.let { BehandlingId(it) },
            vurdertAv = row.getString("VURDERT_AV"),
            opprettet = row.getInstant(
                "OPPRETTET_TID"
            )
        )
    }

    private fun deaktiverEksisterende(behandlingId: BehandlingId) {
        connection.execute("UPDATE PAAKLAGET_BEHANDLING_GRUNNLAG SET AKTIV = FALSE WHERE AKTIV AND BEHANDLING_ID = ?") {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setResultValidator { rowsUpdated ->
                require(rowsUpdated == 1)
            }
        }
    }
}