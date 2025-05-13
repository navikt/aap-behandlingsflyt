package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.samordning.refusjonskrav

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.samordning.refusjonskrav.TjenestepensjonRefusjonsKravVurderingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.samordning.refusjonskrav.TjenestepensjonRefusjonskravVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.Factory
import org.slf4j.LoggerFactory

class TjenestepensjonRefusjonskravVurderingRepositoryImpl(private val connection: DBConnection) : TjenestepensjonRefusjonsKravVurderingRepository {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object : Factory<TjenestepensjonRefusjonskravVurderingRepositoryImpl>{
        override fun konstruer(connection: DBConnection): TjenestepensjonRefusjonskravVurderingRepositoryImpl {
            return TjenestepensjonRefusjonskravVurderingRepositoryImpl(connection)
        }
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): TjenestepensjonRefusjonskravVurdering? {
        val query = """
            SELECT * FROM TJENESTEPENSJON_REFUSJONSKRAV_GRUNNLAG WHERE behandling_id = ? and aktiv = true
        """.trimIndent()

        return connection.queryFirstOrNull(query) {
            setParams {
                setLong(1, behandlingId.id)
            }
            setRowMapper {
                hentTjenestepensjonRefusjonskravVurdering(it.getLong("REFUSJONKRAV_VURDERING_ID"))
            }
        }
    }

    private fun hentTjenestepensjonRefusjonskravVurdering(vurderingId: Long):TjenestepensjonRefusjonskravVurdering{
        val query = """
            SELECT * FROM TJENESTEPENSJON_REFUSJONSKRAV_VURDERING WHERE ID = ?
        """.trimIndent()

        return connection.queryFirst(query) {
            setParams {
                setLong(1, vurderingId)
            }
            setRowMapper {
                TjenestepensjonRefusjonskravVurdering(
                    harKrav = it.getBoolean("HAR_KRAV"),
                    fom = it.getLocalDateOrNull("FOM"),
                    tom = it.getLocalDateOrNull("TOM"),
                    begrunnelse = it.getString("BEGRUNNELSE")
                )
            }
        }
    }
    override fun hent(behandlingId: BehandlingId): TjenestepensjonRefusjonskravVurdering {
        return requireNotNull(hentHvisEksisterer(behandlingId))
    }

    override fun lagre(sakId: SakId, behandlingId: BehandlingId, vurdering: TjenestepensjonRefusjonskravVurdering) {
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)
        if (eksisterendeGrunnlag != null){
            deaktiverEksisterende(behandlingId)
        }

        val vurderingId = lagreVurdering(vurdering)

        val query = """
            INSERT INTO TJENESTEPENSJON_REFUSJONSKRAV_GRUNNLAG (BEHANDLING_ID, SAK_ID, REFUSJONKRAV_VURDERING_ID, AKTIV) VALUES (?, ?, ?, ?)
        """.trimIndent()

        connection.execute(query) {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, sakId.id)
                setLong(3, vurderingId)
            }
        }
    }

    private fun lagreVurdering(vurdering: TjenestepensjonRefusjonskravVurdering): Long{
        val query = """
            INSERT INTO TJENESTEPENSJON_REFUSJONSKRAV_VURDERING (HAR_KRAV, FOM, TOM, BEGRUNNELSE) VALUES (?, ?, ?, ?)
        """.trimIndent()

        return connection.executeReturnKey(query) {
            setParams {
                setBoolean(1, vurdering.harKrav)
                setLocalDate(2, vurdering.fom)
                setLocalDate(3, vurdering.tom)
                setString(4, vurdering.begrunnelse)
            }
        }
    }

    private fun deaktiverEksisterende(behandlingId: BehandlingId) {
        connection.execute("UPDATE TJENESTEPENSJON_REFUSJONSKRAV_GRUNNLAG SET AKTIV = FALSE WHERE AKTIV AND BEHANDLING_ID = ?") {
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
            INSERT INTO TJENESTEPENSJON_REFUSJONSKRAV_GRUNNLAG 
                (BEHANDLING_ID, SAK_ID, REFUSJONKRAV_VURDERING_ID, AKTIV)
            SELECT ?, SAK_ID, REFUSJONKRAV_VURDERING_ID, true
                FROM TJENESTEPENSJON_REFUSJONSKRAV_GRUNNLAG
                WHERE BEHANDLING_ID = ? and aktiv = true
        """.trimIndent()

        connection.execute(query) {
            setParams {
                setLong(1, tilBehandling.id)
                setLong(2, fraBehandling.id)
            }
        }
    }

    override fun slett(behandlingId: BehandlingId) {

        val refusjonsKravVurderingIds = getRefusjonsKravVurderingIds(behandlingId)

        val deletedRows = connection.executeReturnUpdated("""
            delete from tjenestepensjon_refusjonskrav_grunnlag where behandling_id = ?; 
            delete from tjenestepensjon_refusjonskrav_vurdering where id = ANY(?::bigint[]);
        """.trimIndent()) {
            setParams {
                setLong(1, behandlingId.id)
                setLongArray(2, refusjonsKravVurderingIds)
            }
        }
        log.info("Slettet $deletedRows fra tjenestepensjon_refusjonskrav_grunnlag")
    }

    private fun getRefusjonsKravVurderingIds(behandlingId: BehandlingId): List<Long> = connection.queryList(
        """
                    SELECT REFUSJONKRAV_VURDERING_ID
                    FROM tjenestepensjon_refusjonskrav_grunnlag
                    WHERE behandling_id = ?
                 
                """.trimIndent()
    ) {
        setParams { setLong(1, behandlingId.id) }
        setRowMapper { row ->
            row.getLong("refusjonkrav_vurdering_id")
        }
    }

}