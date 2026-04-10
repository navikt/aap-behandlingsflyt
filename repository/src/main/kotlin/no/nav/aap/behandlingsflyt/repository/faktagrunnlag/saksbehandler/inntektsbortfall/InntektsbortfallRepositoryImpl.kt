package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.inntektsbortfall

import no.nav.aap.behandlingsflyt.behandling.inntektsbortfall.InntektsbortfallRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.InntektsbortfallVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.Factory
import org.slf4j.LoggerFactory

class InntektsbortfallRepositoryImpl(private val connection: DBConnection) : InntektsbortfallRepository {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object : Factory<InntektsbortfallRepositoryImpl> {
        override fun konstruer(connection: DBConnection): InntektsbortfallRepositoryImpl {
            return InntektsbortfallRepositoryImpl(connection)
        }
    }

    override fun lagre(
        behandlingId: BehandlingId,
        vurdering: InntektsbortfallVurdering
    ) {
        deaktiverGrunnlag(behandlingId)

        val query = """
            INSERT INTO INNTEKTSBORTFALL_VURDERINGER DEFAULT VALUES;
        """.trimIndent()
        val vurderingerId = connection.executeReturnKey(query)

        connection.execute(
            """
            INSERT INTO INNTEKTSBORTFALL_VURDERING (BEGRUNNELSE, RETT_TIL_ALDERSPENSJON_UTTAK, VURDERINGER_ID, VURDERT_I_BEHANDLING, VURDERT_AV, OPPRETTET_TID)
            VALUES (?, ?, ?, ?, ?, ?)
        """.trimIndent()
        ) {
            setParams {
                setString(1, vurdering.begrunnelse)
                setBoolean(2, vurdering.rettTilUttak)
                setLong(3, vurderingerId)
                setLong(4, vurdering.vurdertIBehandling.id)
                setString(5, vurdering.vurdertAv)
                setLocalDateTime(6, vurdering.opprettetTid)
            }
        }

        connection.execute(
            """
                INSERT INTO INNTEKTSBORTFALL_GRUNNLAG (BEHANDLING_ID, VURDERINGER_ID) VALUES (?, ?)
            """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.id)
                setLong(2, vurderingerId)
            }
        }
    }

    override fun deaktiverGjeldendeVurdering(behandlingId: BehandlingId) {
        deaktiverGrunnlag(behandlingId)
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): InntektsbortfallVurdering? {
        val query = """
            SELECT * FROM INNTEKTSBORTFALL_GRUNNLAG WHERE behandling_id = ? and aktiv = true
        """.trimIndent()

        return connection.queryFirstOrNull(query) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper { row ->
                row.getLong("vurderinger_id").let(::mapVurdering)
            }
        }
    }

    private fun mapVurdering(vurderingerId: Long): InntektsbortfallVurdering? {
        val query = """
            SELECT * FROM INNTEKTSBORTFALL_VURDERING WHERE vurderinger_id = ?
        """.trimIndent()

        return connection.queryFirstOrNull(query) {
            setParams {
                setLong(1, vurderingerId)
            }
            setRowMapper { row ->
                InntektsbortfallVurdering(
                    row.getString("BEGRUNNELSE"),
                    row.getBoolean("RETT_TIL_ALDERSPENSJON_UTTAK"),
                    row.getString("VURDERT_AV"),
                    BehandlingId(row.getLong("VURDERT_I_BEHANDLING")),
                    opprettetTid = row.getLocalDateTime("OPPRETTET_TID"),
                )
            }
        }
    }

    override fun kopier(
        fraBehandling: BehandlingId,
        tilBehandling: BehandlingId
    ) {
        hentHvisEksisterer(fraBehandling) ?: return

        val query = """
            INSERT INTO INNTEKTSBORTFALL_GRUNNLAG (behandling_id, vurderinger_id)
            SELECT ?, vurderinger_id from INNTEKTSBORTFALL_GRUNNLAG where behandling_id = ? and aktiv
        """.trimIndent()

        connection.execute(query) {
            setParams {
                setLong(1, tilBehandling.id)
                setLong(2, fraBehandling.id)
            }
        }
    }

    override fun slett(behandlingId: BehandlingId) {
        val inntektsbortfallVurderingerIds = getInntektsbortfallVurderingerIds(behandlingId)
        val deletedRows = connection.executeReturnUpdated(
            """
            delete from INNTEKTSBORTFALL_VURDERING where vurderinger_id = ANY(?::bigint[]);
            delete from INNTEKTSBORTFALL_GRUNNLAG where behandling_id = ?;
            delete from INNTEKTSBORTFALL_VURDERINGER where id = ANY(?::bigint[]);
        """.trimIndent()
        ) {
            setParams {
                setLongArray(1, inntektsbortfallVurderingerIds)
                setLong(2, behandlingId.id)
                setLongArray(3, inntektsbortfallVurderingerIds)
            }
        }
        log.info("Slettet $deletedRows rader fra INNTEKTSBORTFALL_GRUNNLAG")
    }

    private fun getInntektsbortfallVurderingerIds(behandlingId: BehandlingId): List<Long> = connection.queryList(
        """
            SELECT vurderinger_id
            FROM INNTEKTSBORTFALL_GRUNNLAG
            WHERE behandling_id = ?
        """.trimIndent()
    ) {
        setParams { setLong(1, behandlingId.id) }
        setRowMapper { row ->
            row.getLong("vurderinger_id")
        }
    }

    private fun deaktiverGrunnlag(behandlingId: BehandlingId) {
        connection.execute("UPDATE INNTEKTSBORTFALL_GRUNNLAG set aktiv = false WHERE behandling_id = ? and aktiv = true") {
            setParams {
                setLong(1, behandlingId.id)
            }
        }
    }
}