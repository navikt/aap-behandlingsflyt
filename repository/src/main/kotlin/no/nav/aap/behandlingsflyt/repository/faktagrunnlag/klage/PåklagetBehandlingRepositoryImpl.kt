package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.klage

import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.påklagetbehandling.PåklagetBehandlingGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.påklagetbehandling.PåklagetBehandlingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.påklagetbehandling.PåklagetBehandlingVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.påklagetbehandling.PåklagetBehandlingVurderingMedReferanse
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.lookup.repository.Factory
import org.slf4j.LoggerFactory

class PåklagetBehandlingRepositoryImpl(private val connection: DBConnection) : PåklagetBehandlingRepository {

    private val log = LoggerFactory.getLogger(javaClass)

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

    override fun hentGjeldendeVurderingMedReferanse(behandlingReferanse: BehandlingReferanse): PåklagetBehandlingVurderingMedReferanse? {
        val query = """
            SELECT
                PAAKLAGET_BEHANDLING_VURDERING.type_vedtak as TYPE_VEDTAK,
                PAAKLAGET_BEHANDLING_VURDERING.paaklaget_behandling_id as PAAKLAGET_BEHANDLING_ID,
                PAAKLAGET_BEHANDLING_VURDERING.vurdert_av as VURDERT_AV,
                PAAKLAGET_BEHANDLING_VURDERING.opprettet_tid as OPPRETTET_TID,
                BEHANDLING.referanse as REFERANSE
            FROM PAAKLAGET_BEHANDLING_VURDERING
                 INNER JOIN PAAKLAGET_BEHANDLING_GRUNNLAG ON PAAKLAGET_BEHANDLING_GRUNNLAG.vurdering_id = PAAKLAGET_BEHANDLING_VURDERING.id
                 LEFT JOIN BEHANDLING ON BEHANDLING.id = PAAKLAGET_BEHANDLING_VURDERING.paaklaget_behandling_id
            WHERE PAAKLAGET_BEHANDLING_GRUNNLAG.behandling_id IN (
                SELECT ID FROM BEHANDLING
                WHERE referanse = ?
            ) AND PAAKLAGET_BEHANDLING_GRUNNLAG.aktiv = TRUE;
        """.trimIndent()
        return connection.queryFirstOrNull(query) {
            setParams {
                setUUID(1, behandlingReferanse.referanse)
            }
            setRowMapper(::mapPåklagetBehandlingVurderingMedReferanse)
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

    override fun slett(behandlingId: BehandlingId) {

        val påklagetBehandlingVurderingIds = getpåklagetBehandlingVurderingIds(behandlingId)

        val deletedRows = connection.executeReturnUpdated("""
            delete from PAAKLAGET_BEHANDLING_GRUNNLAG where behandling_id = ?; 
            delete from PAAKLAGET_BEHANDLING_VURDERING where id = ANY(?::bigint[]);
    
           
        """.trimIndent()) {
            setParams {
                setLong(1, behandlingId.id)
                setLongArray(2, påklagetBehandlingVurderingIds)
            }
        }
        log.info("Slettet $deletedRows rader fra PAAKLAGET_BEHANDLING_GRUNNLAG og PAAKLAGET_")
    }

    private fun getpåklagetBehandlingVurderingIds(behandlingId: BehandlingId): List<Long> = connection.queryList(
        """
                    SELECT vurdering_id
                    FROM PAAKLAGET_BEHANDLING_GRUNNLAG
                    WHERE behandling_id = ? AND vurdering_id is not null
                 
                """.trimIndent()
    ) {
        setParams { setLong(1, behandlingId.id) }
        setRowMapper { row ->
            row.getLong("vurdering_id")
        }
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

    private fun mapPåklagetBehandlingVurderingMedReferanse(row: Row): PåklagetBehandlingVurderingMedReferanse {
        return PåklagetBehandlingVurderingMedReferanse(
            påklagetVedtakType = row.getEnum("TYPE_VEDTAK"),
            påklagetBehandling = row.getLongOrNull("PAAKLAGET_BEHANDLING_ID")?.let { BehandlingId(it) },
            vurdertAv = row.getString("VURDERT_AV"),
            referanse = row.getUUIDOrNull("REFERANSE")?.let { BehandlingReferanse(it) },
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