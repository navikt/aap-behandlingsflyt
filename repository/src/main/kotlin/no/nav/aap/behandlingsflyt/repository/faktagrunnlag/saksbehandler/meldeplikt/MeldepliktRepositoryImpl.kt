package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.meldeplikt

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.Fritaksvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.lookup.repository.Factory
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

class MeldepliktRepositoryImpl(private val connection: DBConnection) : MeldepliktRepository {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object : Factory<MeldepliktRepositoryImpl> {
        override fun konstruer(connection: DBConnection): MeldepliktRepositoryImpl {
            return MeldepliktRepositoryImpl(connection)
        }
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): MeldepliktGrunnlag? {
        val harGrunnlag = connection.queryFirstOrNull(
            "SELECT 1 FROM MELDEPLIKT_FRITAK_GRUNNLAG WHERE AKTIV AND BEHANDLING_ID = ?"
        ) {
            setParams { setLong(1, behandlingId.toLong()) }
            setRowMapper { 1 }
        } != null

        if (!harGrunnlag) return null
        return MeldepliktGrunnlag(hentAktiveVurderinger(behandlingId))
    }

    private fun hentAktiveVurderinger(behandlingId: BehandlingId): List<Fritaksvurdering> {
        val query = """
            SELECT v.id as v_id, v.HAR_FRITAK, v.FRA_DATO, v.TIL_DATO, v.BEGRUNNELSE, v.OPPRETTET_TID, v.VURDERT_AV, v.VURDERT_I_BEHANDLING 
            FROM MELDEPLIKT_FRITAK_GRUNNLAG g
            INNER JOIN MELDEPLIKT_FRITAK f ON g.MELDEPLIKT_ID = f.ID
            INNER JOIN MELDEPLIKT_FRITAK_VURDERING v ON f.ID = v.MELDEPLIKT_ID
            WHERE g.AKTIV AND g.BEHANDLING_ID = ?
            """.trimIndent()

        return connection.queryList(query) {
            setParams { setLong(1, behandlingId.toLong()) }
            setRowMapper(::rowToFritaksvurdering)
        }
    }

    private fun rowToFritaksvurdering(row: Row): Fritaksvurdering =
        Fritaksvurdering(
            harFritak = row.getBoolean("HAR_FRITAK"),
            fom = row.getLocalDate("FRA_DATO"),
            tom = row.getLocalDateOrNull("TIL_DATO"),
            begrunnelse = row.getString("BEGRUNNELSE"),
            vurdertAv = row.getString("VURDERT_AV"),
            opprettetTid = row.getLocalDateTime("OPPRETTET_TID"),
            vurdertIBehandling = BehandlingId(row.getLong("VURDERT_I_BEHANDLING"))
        )

    override fun lagre(behandlingId: BehandlingId, vurderinger: List<Fritaksvurdering>) {
        deaktiverEksisterende(behandlingId)
        val meldepliktId = connection.executeReturnKey("INSERT INTO MELDEPLIKT_FRITAK DEFAULT VALUES")

        connection.execute("INSERT INTO MELDEPLIKT_FRITAK_GRUNNLAG (BEHANDLING_ID, MELDEPLIKT_ID) VALUES (?, ?)") {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, meldepliktId)
            }
        }

        connection.executeBatch(
            """
            INSERT INTO MELDEPLIKT_FRITAK_VURDERING 
            (MELDEPLIKT_ID, BEGRUNNELSE, HAR_FRITAK, FRA_DATO, TIL_DATO, VURDERT_AV, OPPRETTET_TID, VURDERT_I_BEHANDLING) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            vurderinger
        ) {
            setParams {
                setLong(1, meldepliktId)
                setString(2, it.begrunnelse)
                setBoolean(3, it.harFritak)
                setLocalDate(4, it.fom)
                setLocalDate(5, it.tom)
                setString(6, it.vurdertAv)
                setLocalDateTime(7, it.opprettetTid)
                setLong(8, it.vurdertIBehandling.id)
            }
        }
    }

    override fun hentFritaksvurderingPåTidspunkt(
        behandlingId: BehandlingId,
        tidspunkt: LocalDateTime
    ): List<Fritaksvurdering>? {
        val fritakIdQuery = """
            SELECT meldeplikt_id
            FROM MELDEPLIKT_FRITAK_GRUNNLAG g
            WHERE g.BEHANDLING_ID = ? AND g.opprettet_tid <= ?
            ORDER BY g.opprettet_tid DESC
            LIMIT 1
            """.trimIndent()

        val fritakId = connection.queryFirstOrNull(
            query = fritakIdQuery,
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
                setLocalDateTime(2, tidspunkt)
            }

            setRowMapper { row ->
                row.getString("meldeplikt_id")
            }
        }

        if (fritakId == null) {
            return null
        }

        val meldepliktVurderingerQuery = """
            SELECT v.HAR_FRITAK, v.FRA_DATO, v.TIL_DATO, v.BEGRUNNELSE, v.OPPRETTET_TID, v.VURDERT_AV, v.VURDERT_I_BEHANDLING 
            FROM MELDEPLIKT_FRITAK f
            INNER JOIN MELDEPLIKT_FRITAK_VURDERING v ON f.ID = v.MELDEPLIKT_ID
            WHERE f.ID = ?
            """.trimIndent()

        return connection.queryList(meldepliktVurderingerQuery) {
            setParams { setLong(1, fritakId.toLong()) }
            setRowMapper(::rowToFritaksvurdering)
        }
    }

    override fun slett(behandlingId: BehandlingId) {

        val meldepliktIds = getMeldepiktIds(behandlingId)

        val deletedRows = connection.executeReturnUpdated(
            """
            delete from meldeplikt_fritak_grunnlag where behandling_id = ?; 
            delete from meldeplikt_fritak_vurdering where meldeplikt_id = ANY(?::bigint[]);
            delete from meldeplikt_fritak where id = ANY(?::bigint[]);
          
        """.trimIndent()
        ) {
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
                    WHERE behandling_id = ?
                 
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

