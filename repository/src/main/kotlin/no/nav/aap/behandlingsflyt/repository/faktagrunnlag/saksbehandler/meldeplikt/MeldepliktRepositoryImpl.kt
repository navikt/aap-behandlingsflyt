package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.meldeplikt

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.Fritaksvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.Factory
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime

class MeldepliktRepositoryImpl(private val connection: DBConnection) : MeldepliktRepository {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object : Factory<MeldepliktRepositoryImpl> {
        override fun konstruer(connection: DBConnection): MeldepliktRepositoryImpl {
            return MeldepliktRepositoryImpl(connection)
        }
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): MeldepliktGrunnlag? {
        val query = """
            SELECT f.ID AS MELDEPLIKT_ID, v.HAR_FRITAK, v.FRA_DATO, v.BEGRUNNELSE, v.OPPRETTET_TID, v.VURDERT_AV
            FROM MELDEPLIKT_FRITAK_GRUNNLAG g
            INNER JOIN MELDEPLIKT_FRITAK f ON g.MELDEPLIKT_ID = f.ID
            INNER JOIN MELDEPLIKT_FRITAK_VURDERING v ON f.ID = v.MELDEPLIKT_ID
            WHERE g.AKTIV AND g.BEHANDLING_ID = ?
            """.trimIndent()

        return connection.queryList(query) {
            setParams { setLong(1, behandlingId.toLong()) }
            setRowMapper { row ->
                MeldepliktInternal(
                    meldepliktId = row.getLong("MELDEPLIKT_ID"),
                    harFritak = row.getBoolean("HAR_FRITAK"),
                    fraDato = row.getLocalDate("FRA_DATO"),
                    begrunnelse = row.getString("BEGRUNNELSE"),
                    vurdertAv = row.getString("VURDERT_AV"),
                    vurderingOpprettet = row.getLocalDateTime("OPPRETTET_TID"),
                )
            }
        }.grupperOgMapTilGrunnlag().firstOrNull()
    }

    override fun hentAlleVurderinger(sakId: SakId, behandlingId: BehandlingId): Set<Fritaksvurdering> {
        val query = """
            SELECT f.ID AS MELDEPLIKT_ID, v.HAR_FRITAK, v.FRA_DATO, v.BEGRUNNELSE, v.OPPRETTET_TID, v.VURDERT_AV
            FROM MELDEPLIKT_FRITAK_GRUNNLAG g
            INNER JOIN MELDEPLIKT_FRITAK f ON g.MELDEPLIKT_ID = f.ID
            INNER JOIN MELDEPLIKT_FRITAK_VURDERING v ON f.ID = v.MELDEPLIKT_ID
            JOIN BEHANDLING b ON b.ID = g.BEHANDLING_ID
            WHERE g.AKTIV AND b.SAK_ID = ? AND b.opprettet_tid < (SELECT a.opprettet_tid from behandling a where id = ?)
            """.trimIndent()

        return connection.queryList(query) {
            setParams {
                setLong(1, sakId.toLong())
                setLong(2, behandlingId.toLong())
            }
            setRowMapper { row ->
                MeldepliktInternal(
                    meldepliktId = row.getLong("MELDEPLIKT_ID"),
                    harFritak = row.getBoolean("HAR_FRITAK"),
                    fraDato = row.getLocalDate("FRA_DATO"),
                    begrunnelse = row.getString("BEGRUNNELSE"),
                    vurdertAv = row.getString("VURDERT_AV"),
                    vurderingOpprettet = row.getLocalDateTime("OPPRETTET_TID")
                )
            }
        }.map { it.toFritaksvurdering() }.toSet()
    }


    private data class MeldepliktInternal(
        val meldepliktId: Long,
        val harFritak: Boolean,
        val fraDato: LocalDate,
        val begrunnelse: String,
        val vurdertAv: String,
        val vurderingOpprettet: LocalDateTime,
    ) {
        fun toFritaksvurdering(): Fritaksvurdering {
            return Fritaksvurdering(
                harFritak = harFritak,
                fraDato = fraDato,
                begrunnelse = begrunnelse,
                vurdertAv = vurdertAv,
                opprettetTid = vurderingOpprettet
            )
        }
    }

    private fun Iterable<MeldepliktInternal>.grupperOgMapTilGrunnlag(): List<MeldepliktGrunnlag> {
        return groupBy(MeldepliktInternal::meldepliktId) { it.toFritaksvurdering() }
            .map { (_, fritaksvurderinger) ->
                MeldepliktGrunnlag(fritaksvurderinger)
            }
    }

    override fun lagre(behandlingId: BehandlingId, vurderinger: List<Fritaksvurdering>) {
        val meldepliktGrunnlag = hentHvisEksisterer(behandlingId)

        if (meldepliktGrunnlag?.vurderinger == vurderinger) return

        if (meldepliktGrunnlag != null) deaktiverEksisterende(behandlingId)

        val meldepliktId = connection.executeReturnKey("INSERT INTO MELDEPLIKT_FRITAK DEFAULT VALUES")

        connection.execute("INSERT INTO MELDEPLIKT_FRITAK_GRUNNLAG (BEHANDLING_ID, MELDEPLIKT_ID) VALUES (?, ?)") {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, meldepliktId)
            }
        }

        val nyeVurderinger = vurderinger.filter { it.opprettetTid == null }

        connection.executeBatch(
            """
            INSERT INTO MELDEPLIKT_FRITAK_VURDERING 
            (MELDEPLIKT_ID, BEGRUNNELSE, HAR_FRITAK, FRA_DATO, VURDERT_AV) VALUES (?, ?, ?, ?, ?)
            """.trimIndent(),
            nyeVurderinger
        ) {
            setParams {
                setLong(1, meldepliktId)
                setString(2, it.begrunnelse)
                setBoolean(3, it.harFritak)
                setLocalDate(4, it.fraDato)
                setString(5, it.vurdertAv)
            }
        }

        connection.executeBatch(
            """
            INSERT INTO MELDEPLIKT_FRITAK_VURDERING 
            (MELDEPLIKT_ID, BEGRUNNELSE, HAR_FRITAK, FRA_DATO, VURDERT_AV, OPPRETTET_TID) VALUES (?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            vurderinger.filter { it.opprettetTid != null }
        ) {
            setParams {
                setLong(1, meldepliktId)
                setString(2, it.begrunnelse)
                setBoolean(3, it.harFritak)
                setLocalDate(4, it.fraDato)
                setString(5, it.vurdertAv)
                setLocalDateTime(6, it.opprettetTid)
            }
        }
    }

    override fun slett(behandlingId: BehandlingId) {

        val meldepliktIds = getMeldepiktIds(behandlingId)

        val deletedRows = connection.executeReturnUpdated("""
            delete from meldeplikt_fritak_grunnlag where behandling_id = ?; 
            delete from meldeplikt_fritak_vurdering where meldeplikt_id = ANY(?::bigint[]);
            delete from meldeplikt_fritak where id = ANY(?::bigint[]);
          
        """.trimIndent()) {
            setParams {
                setLong(1, behandlingId.id)
                setLongArray(2, meldepliktIds)
                setLongArray(3, meldepliktIds)
            }
        }
        log.info("Slettet $deletedRows rader fra meldeplikt_fritak_grunnlag")
    }

    private fun getMeldepiktIds(behandlingId: BehandlingId): List<Long> = connection.queryList(
        """
                    SELECT meldeplikt_id
                    FROM meldeplikt_fritak_grunnlag
                    WHERE behandling_id = ? AND meldeplikt_id is not null
                 
                """.trimIndent()
    ) {
        setParams { setLong(1, behandlingId.id) }
        setRowMapper { row ->
            row.getLong("meldeplikt_id")
        }
    }


    private fun deaktiverEksisterende(behandlingId: BehandlingId) {
        connection.execute("UPDATE MELDEPLIKT_FRITAK_GRUNNLAG SET AKTIV = FALSE WHERE AKTIV AND BEHANDLING_ID = ?") {
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
        connection.execute("INSERT INTO MELDEPLIKT_FRITAK_GRUNNLAG (BEHANDLING_ID, MELDEPLIKT_ID) SELECT ?, MELDEPLIKT_ID FROM MELDEPLIKT_FRITAK_GRUNNLAG WHERE AKTIV AND BEHANDLING_ID = ?") {
            setParams {
                setLong(1, tilBehandling.toLong())
                setLong(2, fraBehandling.toLong())
            }
        }
    }
}
