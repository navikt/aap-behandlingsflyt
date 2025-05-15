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

    override fun hentHvisEksisterer(behandlingId: BehandlingId): RefusjonkravVurdering? {
        val query = """
            SELECT * FROM REFUSJONKRAV_GRUNNLAG WHERE behandling_id = ? and aktiv = true
        """.trimIndent()

        return connection.queryFirstOrNull(query) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper {
                hentRefusjonskrav(it.getLong("refusjonkrav_vurdering_id"))
            }
        }
    }

    private fun hentRefusjonskrav(vurderingId: Long): RefusjonkravVurdering {
        val query = """
            SELECT * FROM REFUSJONKRAV_VURDERING WHERE ID = ?
        """.trimIndent()

        return connection.queryFirst(query) {
            setParams {
                setLong(1, vurderingId)
            }
            setRowMapper {
                RefusjonkravVurdering(
                    harKrav = it.getBoolean("har_krav"),
                    fom = it.getLocalDateOrNull("fom"),
                    tom = it.getLocalDateOrNull("tom")
                )
            }
        }
    }

    override fun hentAlleVurderingerPåSak(sakId: SakId): List<RefusjonkravVurdering> {
        val query = """
            SELECT * FROM REFUSJONKRAV_GRUNNLAG WHERE sak_id = ? and aktiv = true
        """.trimIndent()

        return connection.queryList(query) {
            setParams {
                setLong(1, sakId.id)
            }
            setRowMapper {
                hentRefusjonskrav(it.getLong("REFUSJONKRAV_VURDERING_ID"))
            }
        }
    }

    override fun lagre(sakId: SakId, behandlingId: BehandlingId, refusjonkravVurdering: RefusjonkravVurdering) {
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)
        if (eksisterendeGrunnlag != null) {
            deaktiverEksisterende(behandlingId)
        }

        val vurderingId = lagreVurdering(refusjonkravVurdering)

        val grunnlagQuery = """
            INSERT INTO REFUSJONKRAV_GRUNNLAG (BEHANDLING_ID, SAK_ID, REFUSJONKRAV_VURDERING_ID) VALUES (?, ?, ?)
        """.trimIndent()
        connection.execute(grunnlagQuery) {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, sakId.id)
                setLong(3, vurderingId)
            }
        }
    }

    override fun slett(behandlingId: BehandlingId) {

        val refusjonskravVurderingIds = getRefusjonskravVurderingIds(behandlingId)
        val deletedRows = connection.executeReturnUpdated("""
            delete from REFUSJONKRAV_GRUNNLAG where id = ?;
            delete from REFUSJONKRAV_VURDERING where id = ANY(?::bigint[]);
          
            
        """.trimIndent()) {
            setParams {
                setLong(1, behandlingId.id)
                setLongArray(2, refusjonskravVurderingIds)
            }
        }
        log.info("Slettet $deletedRows fra REFUSJONKRAV_GRUNNLAG")
    }

    private fun getRefusjonskravVurderingIds(behandlingId: BehandlingId): List<Long> = connection.queryList(
        """
                    SELECT refusjonkrav_vurdering_id
                    FROM REFUSJONKRAV_GRUNNLAG
                    WHERE behandling_id = ? AND refusjonkrav_vurdering_id is not null
                 
                """.trimIndent()
    ) {
        setParams { setLong(1, behandlingId.id) }
        setRowMapper { row ->
            row.getLong("refusjonkrav_vurdering_id")
        }
    }


    private fun lagreVurdering(vurdering: RefusjonkravVurdering): Long {
        val query = """
            INSERT INTO REFUSJONKRAV_VURDERING (HAR_KRAV, FOM, TOM) VALUES (?, ?, ?)
        """.trimIndent()

        return connection.executeReturnKey(query) {
            setParams {
                setBoolean(1, vurdering.harKrav)
                setLocalDate(2, vurdering.fom)
                setLocalDate(3, vurdering.tom)
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
                (behandling_id, sak_id, refusjonkrav_vurdering_id) 
            SELECT ?, sak_id, refusjonkrav_vurdering_id
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
