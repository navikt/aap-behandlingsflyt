package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.refusjonkrav

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.refusjonkrav.RefusjonkravRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.refusjonkrav.RefusjonkravVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.Factory
import org.slf4j.LoggerFactory

class RefusjonkravRepositoryImpl(private val connection: DBConnection) : RefusjonkravRepository {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object : Factory<RefusjonkravRepositoryImpl> {
        override fun konstruer(connection: DBConnection): RefusjonkravRepositoryImpl {
            return RefusjonkravRepositoryImpl(connection)
        }
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): List<RefusjonkravVurdering>? {
        val query = """
        SELECT * FROM REFUSJONKRAV_GRUNNLAG WHERE behandling_id = ? AND aktiv = true
    """.trimIndent()

        val vurderingerId = connection.queryFirstOrNull(query) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper {
                it.getLong("refusjonkrav_vurderinger_id")
            }
        }

        return vurderingerId?.let { hentRefusjonkrav(it) }
    }

    private fun hentRefusjonkrav(vurderingerId: Long): List<RefusjonkravVurdering> {
        val query = """
            SELECT * FROM REFUSJONKRAV_VURDERING WHERE REFUSJONKRAV_VURDERINGER_ID = ?
        """.trimIndent()

        return connection.queryList(query) {
            setParams {
                setLong(1, vurderingerId)
            }
            setRowMapper {
                RefusjonkravVurdering(
                    harKrav = it.getBoolean("har_krav"),
                    fom = it.getLocalDateOrNull("fom"),
                    tom = it.getLocalDateOrNull("tom"),
                    vurdertAv = it.getString("vurdert_av"),
                    navKontor = it.getString("navkontor"),
                    opprettetTid = it.getLocalDateTime("opprettet_tid")
                )
            }
        }
    }

    override fun hentAlleVurderingerPåSak(sakId: SakId): List<RefusjonkravVurdering> {
        // TODO: trenger ikke lagre sak_id i tabell
        val query = """
            SELECT * FROM REFUSJONKRAV_GRUNNLAG WHERE sak_id = ? and aktiv = true
        """.trimIndent()

        return connection.queryList(query) {
            setParams {
                setLong(1, sakId.id)
            }
            setRowMapper {
                hentRefusjonkrav(it.getLong("REFUSJONKRAV_VURDERINGER_ID"))
            }
        }.flatten()
    }

    override fun lagre(sakId: SakId, behandlingId: BehandlingId, refusjonkravVurderinger: List<RefusjonkravVurdering>) {
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)
        if (eksisterendeGrunnlag != null) {
            deaktiverEksisterende(behandlingId)
        }

        val vurderingerId = connection.executeReturnKey(
            """
            INSERT INTO REFUSJONKRAV_VURDERINGER DEFAULT VALUES
        """.trimIndent()
        )

        val grunnlagQuery = """
            INSERT INTO REFUSJONKRAV_GRUNNLAG (BEHANDLING_ID, SAK_ID, REFUSJONKRAV_VURDERINGER_ID) VALUES (?, ?, ?)
        """.trimIndent()
        connection.execute(grunnlagQuery) {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, sakId.id)
                setLong(3, vurderingerId)
            }
        }

        refusjonkravVurderinger.forEach { vurdering ->
            lagreVurdering(vurdering, vurderingerId)
        }
    }

    override fun slett(behandlingId: BehandlingId) {
        val refusjonskravVurderingerIds = getRefusjonskravVurderingerIds(behandlingId)

        val deletedRows = connection.executeReturnUpdated("""
            delete from REFUSJONKRAV_GRUNNLAG where behandling_id = ?;
            delete from REFUSJONKRAV_VURDERING where refusjonkrav_vurderinger_id = ANY(?::bigint[]);
            delete from REFUSJONKRAV_VURDERINGER where id = ANY(?::bigint[]);
 
            
        """.trimIndent()) {
            setParams {
                setLong(1, behandlingId.id)
                setLongArray(2, refusjonskravVurderingerIds)
                setLongArray(3, refusjonskravVurderingerIds)
            }
        }
        log.info("Slettet $deletedRows rader fra REFUSJONKRAV_GRUNNLAG")
    }

    private fun getRefusjonskravVurderingIds(behandlingId: BehandlingId): List<Long> = connection.queryList(
        """
                    SELECT refusjonkrav_vurdering_id
                    FROM REFUSJONKRAV_GRUNNLAG
                    WHERE behandling_id = ?
                      AND refusjonkrav_vurdering_id is not null
                 
                """.trimIndent()
    ) {
        setParams { setLong(1, behandlingId.id) }
        setRowMapper { row ->
            row.getLong("refusjonkrav_vurdering_id")
        }
    }

    private fun getRefusjonskravVurderingerIds(behandlingId: BehandlingId): List<Long> = connection.queryList(
        """
                    SELECT refusjonkrav_vurderinger_id
                    FROM REFUSJONKRAV_GRUNNLAG
                    WHERE behandling_id = ?
                      AND refusjonkrav_vurderinger_id is not null
                 
                """.trimIndent()
    ) {
        setParams { setLong(1, behandlingId.id) }
        setRowMapper { row ->
            row.getLong("refusjonkrav_vurderinger_id")
        }
    }

    private fun lagreVurdering(vurdering: RefusjonkravVurdering, vurderingerId: Long): Long {
        val query = """
            INSERT INTO REFUSJONKRAV_VURDERING (HAR_KRAV, FOM, TOM, VURDERT_AV, NAVKONTOR, REFUSJONKRAV_VURDERINGER_ID) VALUES (?, ?, ?, ?, ?, ?)
        """.trimIndent()

        return connection.executeReturnKey(query) {
            setParams {
                setBoolean(1, vurdering.harKrav)
                setLocalDate(2, vurdering.fom)
                setLocalDate(3, vurdering.tom)
                setString(4, vurdering.vurdertAv)
                setString(5, vurdering.navKontor ?: "")
                setLong(6, vurderingerId)
            }
        }
    }

    private fun deaktiverEksisterende(behandlingId: BehandlingId) {
        connection.execute("UPDATE REFUSJONKRAV_GRUNNLAG SET AKTIV = FALSE WHERE AKTIV AND BEHANDLING_ID = ?") {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setResultValidator { rowsUpdated ->
                require(rowsUpdated == 1)
            }
        }
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        hentHvisEksisterer(fraBehandling) ?: return

        val query = """
            INSERT INTO REFUSJONKRAV_GRUNNLAG 
                (behandling_id, sak_id, refusjonkrav_vurderinger_id) 
            SELECT ?, sak_id, refusjonkrav_vurderinger_id
                from REFUSJONKRAV_GRUNNLAG 
                where behandling_id = ? and aktiv
        """.trimIndent()

        connection.execute(query) {
            setParams {
                setLong(1, tilBehandling.toLong())
                setLong(2, fraBehandling.toLong())
            }
        }
    }
}
