package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.arbeidsopptrapping

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsopptrapping.ArbeidsopptrappingGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsopptrapping.ArbeidsopptrappingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsopptrapping.ArbeidsopptrappingVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.Factory
import org.slf4j.LoggerFactory

class ArbeidsopptrappingRepositoryImpl(private val connection: DBConnection) : ArbeidsopptrappingRepository {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object : Factory<ArbeidsopptrappingRepositoryImpl> {
        override fun konstruer(connection: DBConnection): ArbeidsopptrappingRepositoryImpl {
            return ArbeidsopptrappingRepositoryImpl(connection)
        }
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): ArbeidsopptrappingGrunnlag? {
        val query = """
            SELECT * FROM ARBEIDSOPPTRAPPING_GRUNNLAG WHERE behandling_id = ? and aktiv = true
        """.trimIndent()

        return connection.queryFirstOrNull(query) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper { row ->
                ArbeidsopptrappingGrunnlag(
                    row.getLong("vurderinger_id").let(::mapVurdering)
                )
            }
        }
    }

    private fun mapVurdering(vurderingerId: Long): List<ArbeidsopptrappingVurdering> {
        val query = """
            SELECT * FROM ARBEIDSOPPTRAPPING_VURDERING WHERE vurderinger_id = ?
        """.trimIndent()

        return connection.queryList(query) {
            setParams {
                setLong(1, vurderingerId)
            }
            setRowMapper { row ->
                ArbeidsopptrappingVurdering(
                    begrunnelse = row.getString("BEGRUNNELSE"),
                    vurderingenGjelderFra = row.getLocalDate("GJELDER_FRA"),
                    reellMulighetTilOpptrapping = row.getBoolean("MULIGHET_TIL_OPPTRAPPING"),
                    rettPaaAAPIOpptrapping = row.getBoolean("RETT_PAA_AAP"),
                    vurdertAv = row.getString("VURDERT_AV"),
                    opprettetTid = row.getInstant("OPPRETTET_TID"),
                    vurdertIBehandling = BehandlingId(row.getLong("VURDERT_I_BEHANDLING")),
                    vurderingenGjelderTil = row.getLocalDate("GJELDER_TIL")
                )
            }
        }
    }

    override fun lagre(
        behandlingId: BehandlingId,
        arbeidsopptrappingVurdering: List<ArbeidsopptrappingVurdering>
    ) {
        deaktiverGrunnlag(behandlingId)

        val query = """
            INSERT INTO ARBEIDSOPPTRAPPING_VURDERINGER DEFAULT VALUES;
        """.trimIndent()
        val vurderingerId = connection.executeReturnKey(query)

        connection.executeBatch(
            """
            INSERT INTO ARBEIDSOPPTRAPPING_VURDERING (BEGRUNNELSE, MULIGHET_TIL_OPPTRAPPING, RETT_PAA_AAP, VURDERINGER_ID, VURDERT_I_BEHANDLING, VURDERT_AV, GJELDER_FRA, GJELDER_TIL)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent(), arbeidsopptrappingVurdering
        ) {
            setParams {
                setString(1, it.begrunnelse)
                setBoolean(2, it.reellMulighetTilOpptrapping)
                setBoolean(3, it.rettPaaAAPIOpptrapping)
                setLong(4, vurderingerId)
                setLong(5, it.vurdertIBehandling.id)
                setString(6, it.vurdertAv)
                setLocalDate(7, it.vurderingenGjelderFra)
                setLocalDate(7, it.vurderingenGjelderTil)
            }
        }

        connection.execute(
            """
                INSERT INTO ARBEIDSOPPTRAPPING_GRUNNLAG (BEHANDLING_ID, VURDERINGER_ID) VALUES (?, ?)
            """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.id)
                setLong(2, vurderingerId)
            }
        }
    }

    override fun kopier(
        fraBehandling: BehandlingId,
        tilBehandling: BehandlingId
    ) {
        hentHvisEksisterer(fraBehandling) ?: return

        val query = """
            INSERT INTO ARBEIDSOPPTRAPPING_GRUNNLAG (behandling_id, vurderinger_id)
            SELECT ?, vurderinger_id from ARBEIDSOPPTRAPPING_GRUNNLAG where behandling_id = ? and aktiv
        """.trimIndent()

        connection.execute(query) {
            setParams {
                setLong(1, tilBehandling.id)
                setLong(2, fraBehandling.id)
            }
        }
    }

    override fun slett(behandlingId: BehandlingId) {
        val arbeidsopptrappingVurderingerIds = getArbeidsopptrappingVurderingerIds(behandlingId)
        val deletedRows = connection.executeReturnUpdated(
            """
            delete from ARBEIDSOPPTRAPPING_GRUNNLAG where behandling_id = ?; 
            delete from ARBEIDSOPPTRAPPING_VURDERING where id = ANY(?::bigint[]);
            delete from ARBEIDSOPPTRAPPING_VURDERINGER where id in (select id from ARBEIDSOPPTRAPPING_GRUNNLAG where behandling_id = ?)
        """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.id)
                setLongArray(2, arbeidsopptrappingVurderingerIds)
                setLongArray(3, arbeidsopptrappingVurderingerIds)
                setLong(4, behandlingId.id)
            }
        }
        log.info("Slettet $deletedRows rader fra ARBEIDSOPPTRAPPING_GRUNNLAG")
    }

    private fun getArbeidsopptrappingVurderingerIds(behandlingId: BehandlingId): List<Long> = connection.queryList(
        """
            SELECT vurderinger_id
            FROM ARBEIDSOPPTRAPPING_GRUNNLAG
            WHERE behandling_id = ?
        """.trimIndent()
    ) {
        setParams { setLong(1, behandlingId.id) }
        setRowMapper { row ->
            row.getLong("vurderinger_id")
        }
    }

    private fun deaktiverGrunnlag(behandlingId: BehandlingId) {
        connection.execute("UPDATE ARBEIDSOPPTRAPPING_GRUNNLAG set aktiv = false WHERE behandling_id = ? and aktiv = true") {
            setParams {
                setLong(1, behandlingId.id)
            }
            setResultValidator { require(it == 1) }
        }
    }
}