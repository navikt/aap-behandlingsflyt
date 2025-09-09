package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.meldeplikt

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktOverstyringStatus
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.OverstyringMeldepliktGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.OverstyringMeldepliktRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.OverstyringMeldepliktVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.OverstyringMeldepliktVurderingPeriode
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.Factory
import org.intellij.lang.annotations.Language
import java.time.LocalDateTime
import java.util.UUID

class OverstyringMeldepliktRepositoryImpl(private val connection: DBConnection) : OverstyringMeldepliktRepository {
    companion object : Factory<OverstyringMeldepliktRepositoryImpl> {
        override fun konstruer(connection: DBConnection): OverstyringMeldepliktRepositoryImpl {
            return OverstyringMeldepliktRepositoryImpl(connection)
        }
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): OverstyringMeldepliktGrunnlag? {
        return hentGrunnlagInternalHvisEksisterer(behandlingId)?.grupperOgMapTilGrunnlag()
    }

    private fun hentGrunnlagInternalHvisEksisterer(behandlingId: BehandlingId): List<MeldepliktOverstyringInternal>? {
        val query = """
            SELECT g.ID AS GRUNNLAG_ID, v.ID AS VURDERING_ID, v.OPPRETTET_TID, v.VURDERT_AV, p.PERIODE, p.MELDEPLIKT_OVERSTYRING_STATUS, p.BEGRUNNELSE, b.referanse AS VURDERT_I_BEHANDLING
            FROM MELDEPLIKT_OVERSTYRING_GRUNNLAG g
            INNER JOIN MELDEPLIKT_OVERSTYRING_VURDERINGER vurderinger ON vurderinger.GRUNNLAG_ID = g.ID
            INNER JOIN MELDEPLIKT_OVERSTYRING_VURDERING v ON vurderinger.VURDERING_ID = v.ID
            INNER JOIN BEHANDLING b on b.ID = v.VURDERT_I_BEHANDLING
            INNER JOIN MELDEPLIKT_OVERSTYRING_VURDERING_PERIODER p ON v.ID = p.MELDEPLIKT_OVERSTYRING_VURDERING_ID
            WHERE g.BEHANDLING_ID = ? AND g.AKTIV = true
        """.trimIndent()

        return connection.queryList(query) {
            setParams { setLong(1, behandlingId.toLong()) }
            setRowMapper { row ->
                MeldepliktOverstyringInternal(
                    grunnlagId = row.getLong("GRUNNLAG_ID"),
                    vurderingId = row.getLong("VURDERING_ID"),
                    meldepliktOverstyringStatus = row.getEnum("MELDEPLIKT_OVERSTYRING_STATUS"),
                    periode = row.getPeriode("PERIODE"),
                    begrunnelse = row.getString("BEGRUNNELSE"),
                    vurdertAv = row.getString("VURDERT_AV"),
                    vurdertIBehandling = row.getUUID("VURDERT_I_BEHANDLING"),
                    vurderingOpprettet = row.getLocalDateTime("OPPRETTET_TID"),
                )
            }
        }
    }

    private fun hentIdForAktivtGrunnlagForBehandling(behandlingId: BehandlingId): Long? {
        val query = """
            SELECT ID
            FROM MELDEPLIKT_OVERSTYRING_GRUNNLAG
            WHERE BEHANDLING_ID = ? AND AKTIV = true;
        """.trimIndent()

        return connection.queryFirstOrNull(query) {
            setParams { setLong(1, behandlingId.toLong()) }
            setRowMapper { it.getLong("ID") }
        }
    }

    override fun lagre(behandlingId: BehandlingId, vurdering: OverstyringMeldepliktVurdering) {
        val grunnlagId = opprettNyttGrunnlag(behandlingId)
        insertVurderingMedPerioder(grunnlagId, vurdering.vurdertAv,  vurdering.vurdertIBehandling, vurdering.perioder)
    }

    override fun slett(behandlingId: BehandlingId) {
        val grunnlagId = hentIdForAktivtGrunnlagForBehandling(behandlingId) ?: return

        val vurderingIder: List<Long> = connection.queryList("""
            SELECT vurdering_id
            FROM MELDEPLIKT_OVERSTYRING_VURDERINGER
            WHERE grunnlag_id = ?
            """.trimIndent()
        ) {
            setParams { setLong(1, grunnlagId) }
            setRowMapper { it.getLong("vurdering_id") }
        }

        connection.execute("""
            DELETE FROM MELDEPLIKT_OVERSTYRING_VURDERINGER
            WHERE grunnlag_id = ?
            """.trimIndent()
        ) { setParams { setLong(1, grunnlagId) } }

        if (vurderingIder.isNotEmpty()) {
            // Finn alle vurderinger som nå er orphan (ikke lenger referert til av andre grunnlag), som betyr at de KUN var en del av det slettede grunnlaget
            val orphanVurderinger: List<Long> = connection.queryList("""
                SELECT v.id AS orphan_id
                FROM MELDEPLIKT_OVERSTYRING_VURDERING v
                WHERE v.id = ANY (?)
                  AND NOT EXISTS (
                      SELECT 1
                      FROM MELDEPLIKT_OVERSTYRING_VURDERINGER mv
                      WHERE mv.vurdering_id = v.id
                  )
                """.trimIndent()
            ) {
                setParams { setLongArray(1, vurderingIder) }
                setRowMapper { it.getLong("orphan_id") }
            }

            if (orphanVurderinger.isNotEmpty()) {
                connection.execute("""
                    DELETE FROM MELDEPLIKT_OVERSTYRING_VURDERING_PERIODER
                    WHERE MELDEPLIKT_OVERSTYRING_VURDERING_ID = ANY (?)
                    """.trimIndent()
                ) { setParams { setLongArray(1, orphanVurderinger) } }

                // 5) Slett selve vurderingene i én operasjon
                connection.execute("""
                    DELETE FROM MELDEPLIKT_OVERSTYRING_VURDERING
                    WHERE id = ANY (?)
                    """.trimIndent()
                ) { setParams { setLongArray(1, orphanVurderinger) } }
            }
        }

        connection.execute(
            """
            DELETE FROM MELDEPLIKT_OVERSTYRING_GRUNNLAG
            WHERE id = ?
            """.trimIndent()
        ) { setParams { setLong(1, grunnlagId) } }
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        require(fraBehandling != tilBehandling)

        val grunnlagId = connection.executeReturnKey("INSERT INTO MELDEPLIKT_OVERSTYRING_GRUNNLAG(behandling_id, aktiv) VALUES(?, true)") {
            setParams { setLong(1, tilBehandling.toLong()) }
        }

        val queryKopierGamleVurderinger = """
            INSERT INTO MELDEPLIKT_OVERSTYRING_VURDERINGER (grunnlag_id, vurdering_id) (
                SELECT ?, vurdering.id
                FROM MELDEPLIKT_OVERSTYRING_GRUNNLAG grunnlag
                INNER JOIN MELDEPLIKT_OVERSTYRING_VURDERINGER vurderinger ON grunnlag.id = vurderinger.grunnlag_id
                INNER JOIN MELDEPLIKT_OVERSTYRING_VURDERING vurdering ON vurdering.id = vurderinger.vurdering_id
                WHERE grunnlag.aktiv = true AND grunnlag.behandling_id=?
            )
        """.trimIndent()

        connection.execute(queryKopierGamleVurderinger) {
            setParams {
                setLong(1, grunnlagId)
                setLong(2, fraBehandling.toLong())
            }
        }
    }

    private fun insertVurderingMedPerioder(grunnlagId: Long, vurdertAv: String, vurdertI: BehandlingReferanse, perioder: List<OverstyringMeldepliktVurderingPeriode>) {
        val queryGetBehandlingId = """
            SELECT id
            FROM behandling
            WHERE referanse=?
        """.trimIndent()

        val queryInsertVurdering = """
            INSERT INTO MELDEPLIKT_OVERSTYRING_VURDERING(VURDERT_AV, VURDERT_I_BEHANDLING)
            VALUES(?, ?)
        """.trimIndent()

        val queryInsertPeriode = """
            INSERT INTO MELDEPLIKT_OVERSTYRING_VURDERING_PERIODER(MELDEPLIKT_OVERSTYRING_VURDERING_ID, MELDEPLIKT_OVERSTYRING_STATUS, PERIODE, BEGRUNNELSE)
            VALUES(?, ?, ?::daterange, ?)
        """.trimIndent()

        val queryInsertVurderinger = """
            INSERT INTO MELDEPLIKT_OVERSTYRING_VURDERINGER(GRUNNLAG_ID, VURDERING_ID)
            VALUES(?, ?)
        """.trimIndent()

        val behadlingId = connection.queryFirst(queryGetBehandlingId) {
            setParams { setUUID(1, vurdertI.referanse) }
            setRowMapper { it.getLong("id") }
        }

        val vurderingId = connection.executeReturnKey(queryInsertVurdering) {
            setParams {
                setString(1, vurdertAv)
                setLong(2, behadlingId)
            }
        }

        connection.executeBatch(queryInsertPeriode, perioder) {
            setParams { periode ->
                setLong(1, vurderingId)
                setString(2, periode.meldepliktOverstyringStatus.value)
                setPeriode(3, Periode(periode.fom, periode.tom))
                setString(4, periode.begrunnelse)
            }
        }

        connection.execute(queryInsertVurderinger) {
            setParams {
                setLong(1, grunnlagId)
                setLong(2, vurderingId)
            }
        }
    }

    private fun opprettNyttGrunnlag(behandlingId: BehandlingId): Long {
        val gammeltGrunnlagId = hentIdForAktivtGrunnlagForBehandling(behandlingId)

        if (gammeltGrunnlagId != null) {
            connection.execute("""
                UPDATE MELDEPLIKT_OVERSTYRING_GRUNNLAG
                SET AKTIV=false
                WHERE ID=?
            """.trimIndent()) {
                setParams { setLong(1, gammeltGrunnlagId) }
            }
        }

        val queryInsertNyttGrunnlag = """
            INSERT INTO MELDEPLIKT_OVERSTYRING_GRUNNLAG(BEHANDLING_ID, AKTIV) VALUES(?, TRUE);
        """.trimIndent()

        val grunnlagId = connection.executeReturnKey(queryInsertNyttGrunnlag) {
            setParams { setLong(1, behandlingId.toLong()) }
        }

        if (gammeltGrunnlagId != null) {
            // Vi må også kopiere over alle vurderingene fra det gamle grunnlaget.
            // Men vi ønsker å kun beholde vurderingene som er vedtatt, "ny vurderinger" skal overskrives
            // med den nye vurderingen som er sendt inn og lagres på dette grunnlaget
            connection.execute("""
                INSERT INTO MELDEPLIKT_OVERSTYRING_VURDERINGER (GRUNNLAG_ID, VURDERING_ID) (
                    SELECT ? AS GRUNNLAG_ID, vurdering.ID 
                    FROM MELDEPLIKT_OVERSTYRING_VURDERINGER vurderinger
                    INNER JOIN MELDEPLIKT_OVERSTYRING_VURDERING vurdering ON vurderinger.VURDERING_ID = vurdering.ID
                    WHERE vurdering.VURDERT_I_BEHANDLING != ? 
                    AND vurderinger.GRUNNLAG_ID = ?
                )
            """.trimIndent()) {
                setParams {
                    setLong(1, grunnlagId)
                    setLong(2, behandlingId.toLong())
                    setLong(3, gammeltGrunnlagId)
                }
            }
        }

        return grunnlagId
    }

    private data class MeldepliktOverstyringInternal(
        val grunnlagId: Long,
        val vurderingId: Long,
        val vurdertIBehandling: UUID,
        val meldepliktOverstyringStatus: MeldepliktOverstyringStatus,
        val periode: Periode,
        val begrunnelse: String,
        val vurdertAv: String,
        val vurderingOpprettet: LocalDateTime,
    )

    private fun Iterable<MeldepliktOverstyringInternal>.grupperOgMapTilGrunnlag(): OverstyringMeldepliktGrunnlag? {
        if (this.count() == 0) {
            return null
        }

        val vurderinger = groupBy(MeldepliktOverstyringInternal::vurderingId)
            .map { (_, vurderingGroup) ->
                val firstVudering = vurderingGroup.first()
                OverstyringMeldepliktVurdering(
                    vurdertAv = firstVudering.vurdertAv,
                    opprettetTid = firstVudering.vurderingOpprettet,
                    vurdertIBehandling = BehandlingReferanse(firstVudering.vurdertIBehandling),
                    perioder = vurderingGroup.map { it.toPeriode() }
                )
            }
        return OverstyringMeldepliktGrunnlag(vurderinger)
    }

    private fun MeldepliktOverstyringInternal.toPeriode(): OverstyringMeldepliktVurderingPeriode =
        OverstyringMeldepliktVurderingPeriode(
            fom = periode.fom,
            tom = periode.tom,
            begrunnelse = begrunnelse,
            meldepliktOverstyringStatus = meldepliktOverstyringStatus,
        )
}
