package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.meldeplikt

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktRimeligGrunnGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktRimeligGrunnRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.RimeligGrunnVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.Factory
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime

class MeldepliktRimeligGrunnRepositoryImpl(private val connection: DBConnection) : MeldepliktRimeligGrunnRepository {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object : Factory<MeldepliktRimeligGrunnRepositoryImpl> {
        override fun konstruer(connection: DBConnection): MeldepliktRimeligGrunnRepositoryImpl {
            return MeldepliktRimeligGrunnRepositoryImpl(connection)
        }
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): MeldepliktRimeligGrunnGrunnlag? {
        val query = """
            SELECT f.ID AS MELDEPLIKT_RIMELIG_GRUNN_ID, v.HAR_RIMELIG_GRUNN, v.FRA_DATO, v.BEGRUNNELSE, v.OPPRETTET_TID, v.VURDERT_AV
            FROM MELDEPLIKT_RIMELIG_GRUNN_GRUNNLAG g
            INNER JOIN MELDEPLIKT_RIMELIG_GRUNN f ON g.MELDEPLIKT_RIMELIG_GRUNN_ID = f.ID
            INNER JOIN MELDEPLIKT_RIMELIG_GRUNN_VURDERING v ON f.ID = v.MELDEPLIKT_RIMELIG_GRUNN_ID
            WHERE g.AKTIV AND g.BEHANDLING_ID = ?
            """.trimIndent()

        return connection.queryList(query) {
            setParams { setLong(1, behandlingId.toLong()) }
            setRowMapper { row ->
                MeldepliktRimeligGrunnInternal(
                    meldepliktRimeligGrunnId = row.getLong("MELDEPLIKT_RIMELIG_GRUNN_ID"),
                    harRimeligGrunn = row.getBoolean("HAR_RIMELIG_GRUNN"),
                    fraDato = row.getLocalDate("FRA_DATO"),
                    begrunnelse = row.getString("BEGRUNNELSE"),
                    vurdertAv = row.getString("VURDERT_AV"),
                    vurderingOpprettet = row.getLocalDateTime("OPPRETTET_TID"),
                )
            }
        }.grupperOgMapTilGrunnlag().firstOrNull()
    }

    override fun hentAlleVurderinger(sakId: SakId, behandlingId: BehandlingId): Set<RimeligGrunnVurdering> {
        val query = """
            SELECT f.ID AS MELDEPLIKT_RIMELIG_GRUNN_ID, v.HAR_RIMELIG_GRUNN, v.FRA_DATO, v.BEGRUNNELSE, v.OPPRETTET_TID, v.VURDERT_AV
            FROM MELDEPLIKT_RIMELIG_GRUNN_GRUNNLAG g
            INNER JOIN MELDEPLIKT_RIMELIG_GRUNN f ON g.MELDEPLIKT_RIMELIG_GRUNN_ID = f.ID
            INNER JOIN MELDEPLIKT_RIMELIG_GRUNN_VURDERING v ON f.ID = v.MELDEPLIKT_RIMELIG_GRUNN_ID
            JOIN BEHANDLING b ON b.ID = g.BEHANDLING_ID
            WHERE g.AKTIV AND b.SAK_ID = ? AND b.opprettet_tid < (SELECT a.opprettet_tid from behandling a where id = ?)
            """.trimIndent()

        return connection.queryList(query) {
            setParams {
                setLong(1, sakId.toLong())
                setLong(2, behandlingId.toLong())
            }
            setRowMapper { row ->
                MeldepliktRimeligGrunnInternal(
                    meldepliktRimeligGrunnId = row.getLong("MELDEPLIKT_RIMELIG_GRUNN_ID"),
                    harRimeligGrunn = row.getBoolean("HAR_RIMELIG_GRUNN"),
                    fraDato = row.getLocalDate("FRA_DATO"),
                    begrunnelse = row.getString("BEGRUNNELSE"),
                    vurdertAv = row.getString("VURDERT_AV"),
                    vurderingOpprettet = row.getLocalDateTime("OPPRETTET_TID")
                )
            }
        }.map { it.toRimeligGrunnVurdering() }.toSet()
    }


    private data class MeldepliktRimeligGrunnInternal(
        val meldepliktRimeligGrunnId: Long,
        val harRimeligGrunn: Boolean,
        val fraDato: LocalDate,
        val begrunnelse: String,
        val vurdertAv: String,
        val vurderingOpprettet: LocalDateTime,
    ) {
        fun toRimeligGrunnVurdering(): RimeligGrunnVurdering {
            return RimeligGrunnVurdering(
                harRimeligGrunn = harRimeligGrunn,
                fraDato = fraDato,
                begrunnelse = begrunnelse,
                vurdertAv = vurdertAv,
                opprettetTid = vurderingOpprettet
            )
        }
    }

    private fun Iterable<MeldepliktRimeligGrunnInternal>.grupperOgMapTilGrunnlag(): List<MeldepliktRimeligGrunnGrunnlag> {
        return groupBy(MeldepliktRimeligGrunnInternal::meldepliktRimeligGrunnId) { it.toRimeligGrunnVurdering() }
            .map { (_, rimeligGrunnVurderinger) ->
                MeldepliktRimeligGrunnGrunnlag(rimeligGrunnVurderinger)
            }
    }

    override fun lagre(behandlingId: BehandlingId, vurderinger: List<RimeligGrunnVurdering>) {
        val meldepliktRimeligGrunnGrunnlag = hentHvisEksisterer(behandlingId)

        if (meldepliktRimeligGrunnGrunnlag?.vurderinger == vurderinger) return

        if (meldepliktRimeligGrunnGrunnlag != null) deaktiverEksisterende(behandlingId)

        val meldepliktRimeligGrunnId = connection.executeReturnKey("INSERT INTO MELDEPLIKT_RIMELIG_GRUNN DEFAULT VALUES")

        connection.execute("INSERT INTO MELDEPLIKT_RIMELIG_GRUNN_GRUNNLAG (BEHANDLING_ID, MELDEPLIKT_RIMELIG_GRUNN_ID) VALUES (?, ?)") {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, meldepliktRimeligGrunnId)
            }
        }

        val nyeVurderinger = vurderinger.filter { it.opprettetTid == null }

        connection.executeBatch(
            """
            INSERT INTO MELDEPLIKT_RIMELIG_GRUNN_VURDERING 
            (MELDEPLIKT_RIMELIG_GRUNN_ID, BEGRUNNELSE, HAR_RIMELIG_GRUNN, FRA_DATO, VURDERT_AV) VALUES (?, ?, ?, ?, ?)
            """.trimIndent(),
            nyeVurderinger
        ) {
            setParams {
                setLong(1, meldepliktRimeligGrunnId)
                setString(2, it.begrunnelse)
                setBoolean(3, it.harRimeligGrunn)
                setLocalDate(4, it.fraDato)
                setString(5, it.vurdertAv)
            }
        }

        connection.executeBatch(
            """
            INSERT INTO MELDEPLIKT_RIMELIG_GRUNN_VURDERING 
            (MELDEPLIKT_RIMELIG_GRUNN_ID, BEGRUNNELSE, HAR_RIMELIG_GRUNN, FRA_DATO, VURDERT_AV, OPPRETTET_TID) VALUES (?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            vurderinger.filter { it.opprettetTid != null }
        ) {
            setParams {
                setLong(1, meldepliktRimeligGrunnId)
                setString(2, it.begrunnelse)
                setBoolean(3, it.harRimeligGrunn)
                setLocalDate(4, it.fraDato)
                setString(5, it.vurdertAv)
                setLocalDateTime(6, it.opprettetTid)
            }
        }
    }

    override fun slett(behandlingId: BehandlingId) {

        val meldepliktIds = getMeldepiktIds(behandlingId)

        val deletedRows = connection.executeReturnUpdated("""
            delete from meldeplikt_rimelig_grunn_grunnlag where behandling_id = ?; 
            delete from meldeplikt_rimelig_grunn_vurdering where meldeplikt_rimelig_grunn_id = ANY(?::bigint[]);
            delete from meldeplikt_rimelig_grunn where id = ANY(?::bigint[]);
          
        """.trimIndent()) {
            setParams {
                setLong(1, behandlingId.id)
                setLongArray(2, meldepliktIds)
                setLongArray(3, meldepliktIds)
            }
        }
        log.info("Slettet $deletedRows rader fra meldeplikt_rimelig_grunn_grunnlag")
    }

    private fun getMeldepiktIds(behandlingId: BehandlingId): List<Long> = connection.queryList(
        """
                    SELECT meldeplikt_rimelig_grunn_id
                    FROM meldeplikt_rimelig_grunn_grunnlag
                    WHERE behandling_id = ? AND meldeplikt_rimelig_grunn_id is not null
                 
                """.trimIndent()
    ) {
        setParams { setLong(1, behandlingId.id) }
        setRowMapper { row ->
            row.getLong("meldeplikt_rimelig_grunn_id")
        }
    }


    private fun deaktiverEksisterende(behandlingId: BehandlingId) {
        connection.execute("UPDATE MELDEPLIKT_RIMELIG_GRUNN_GRUNNLAG SET AKTIV = FALSE WHERE AKTIV AND BEHANDLING_ID = ?") {
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
        connection.execute("INSERT INTO MELDEPLIKT_RIMELIG_GRUNN_GRUNNLAG (BEHANDLING_ID, MELDEPLIKT_RIMELIG_GRUNN_ID) SELECT ?, MELDEPLIKT_RIMELIG_GRUNN_ID FROM MELDEPLIKT_RIMELIG_GRUNN_GRUNNLAG WHERE AKTIV AND BEHANDLING_ID = ?") {
            setParams {
                setLong(1, tilBehandling.toLong())
                setLong(2, fraBehandling.toLong())
            }
        }
    }
}
