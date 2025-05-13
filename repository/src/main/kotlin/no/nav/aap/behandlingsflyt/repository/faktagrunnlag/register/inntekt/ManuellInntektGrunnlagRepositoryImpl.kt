package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.register.inntekt

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.ManuellInntektGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.ManuellInntektGrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.ManuellInntektVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.Factory
import org.slf4j.LoggerFactory
import java.time.Year

class ManuellInntektGrunnlagRepositoryImpl(private val connection: DBConnection) : ManuellInntektGrunnlagRepository {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object : Factory<ManuellInntektGrunnlagRepositoryImpl> {
        override fun konstruer(connection: DBConnection): ManuellInntektGrunnlagRepositoryImpl {
            return ManuellInntektGrunnlagRepositoryImpl(connection)
        }
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): ManuellInntektGrunnlag? {
        val query = """
            SELECT * FROM MANUELL_INNTEKT_VURDERING_GRUNNLAG WHERE behandling_id = ? and aktiv = true
        """.trimIndent()

        return connection.queryFirstOrNull(query) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper {
                ManuellInntektGrunnlag(
                    manuelleInntekter = hentManuellInntektVurderinger(it.getLong("MANUELL_INNTEKT_VURDERINGER_ID"))
                )
            }
        }
    }

    private fun hentManuellInntektVurderinger(vurderingerId: Long): Set<ManuellInntektVurdering> {
        val query = """
            SELECT * FROM MANUELL_INNTEKT_VURDERING WHERE MANUELL_INNTEKT_VURDERINGER_ID = ?
        """.trimIndent()

        return connection.querySet(query) {
            setParams {
                setLong(1, vurderingerId)
            }
            setRowMapper {
                ManuellInntektVurdering(
                    år = Year.of(it.getInt("ar")),
                    begrunnelse = it.getString("begrunnelse"),
                    belop = it.getBigDecimal("belop"),
                    vurdertAv = it.getString("vurdert_av")
                )
            }
        }
    }

    override fun lagre(sakId: SakId, behandlingId: BehandlingId, manuellVurdering: ManuellInntektVurdering) {
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)
        if (eksisterendeGrunnlag != null) {
            deaktiverEksisterende(behandlingId)
        }

        val manuellInntektVurderingerQuery = """
            INSERT INTO MANUELL_INNTEKT_VURDERINGER DEFAULT VALUES
        """.trimIndent()
        val manuellInntektVurderingerId = connection.executeReturnKey(manuellInntektVurderingerQuery)
        lagreVurdering(manuellVurdering, eksisterendeGrunnlag?.manuelleInntekter, manuellInntektVurderingerId)

        val grunnlagQuery = """
            INSERT INTO MANUELL_INNTEKT_VURDERING_GRUNNLAG (BEHANDLING_ID, SAK_ID, MANUELL_INNTEKT_VURDERINGER_ID) VALUES (?, ?, ?)
        """.trimIndent()
        connection.execute(grunnlagQuery) {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, sakId.id)
                setLong(3, manuellInntektVurderingerId)
            }
        }
    }

    private fun lagreVurdering(
        vurdering: ManuellInntektVurdering,
        eksisterendeVurderinger: Set<ManuellInntektVurdering>?,
        manuellInntektVurderingerId: Long
    ) {
        val kombinerteEntries = (eksisterendeVurderinger.orEmpty()
            .filterNot { it.år == vurdering.år } + vurdering).toSet()

        val query = """
            INSERT INTO MANUELL_INNTEKT_VURDERING (AR, BEGRUNNELSE, BELOP, VURDERT_AV, MANUELL_INNTEKT_VURDERINGER_ID) VALUES (?, ?, ?, ?, ?)
        """.trimIndent()

        connection.executeBatch(query, kombinerteEntries) {
            setParams {
                setInt(1, it.år.value)
                setString(2, it.begrunnelse)
                setBigDecimal(3, it.belop)
                setString(4, it.vurdertAv)
                setLong(5, manuellInntektVurderingerId)
            }
        }
    }

    private fun hentVurderingerIds(behandlingId: BehandlingId): List<Long> = connection.queryList(
        """
            SELECT MANUELL_INNTEKT_VURDERINGER_ID
            FROM MANUELL_INNTEKT_VURDERING_GRUNNLAG
            WHERE behandling_id = ? AND MANUELL_INNTEKT_VURDERINGER_ID is not null
         """.trimIndent()
    ) {
        setParams { setLong(1, behandlingId.id) }
        setRowMapper { row ->
            row.getLong("MANUELL_INNTEKT_VURDERINGER_ID")
        }
    }

    override fun slett(behandlingId: BehandlingId) {
        val manuellInntektVurderingerIds = hentVurderingerIds(behandlingId)

        val deletedRows = connection.executeReturnUpdated(
            """
            delete from MANUELL_INNTEKT_VURDERING_GRUNNLAG where id = ?;
            delete from MANUELL_INNTEKT_VURDERING where id = ANY(?::bigint[]);  
        """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.id)
                setLongArray(2, manuellInntektVurderingerIds)
            }
        }
        log.info("Slettet $deletedRows fra MANUELL_INNTEKT_VURDERING_GRUNNLAG")
    }

    private fun deaktiverEksisterende(behandlingId: BehandlingId) {
        connection.execute("UPDATE MANUELL_INNTEKT_VURDERING_GRUNNLAG SET AKTIV = FALSE WHERE AKTIV AND BEHANDLING_ID = ?") {
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
            INSERT INTO MANUELL_INNTEKT_VURDERING_GRUNNLAG 
                (behandling_id, sak_id, MANUELL_INNTEKT_VURDERINGER_ID) 
            SELECT ?, sak_id, MANUELL_INNTEKT_VURDERINGER_ID
                from MANUELL_INNTEKT_VURDERING_GRUNNLAG 
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
