package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.register.inntekt

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.ManuellInntektGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.ManuellInntektGrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.ManuellInntektVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.lookup.repository.Factory
import org.slf4j.LoggerFactory
import java.time.Year
import kotlin.collections.filterNot
import kotlin.collections.orEmpty
import kotlin.collections.plus
import kotlin.collections.toSet

class ManuellInntektGrunnlagRepositoryImpl(private val connection: DBConnection) :
    ManuellInntektGrunnlagRepository {
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

        val manuellInntektVurderingId = connection.queryFirstOrNull(query) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper {
                it.getLong("MANUELL_INNTEKT_VURDERINGER_ID")
            }
        }

        if (manuellInntektVurderingId == null) return null

        return ManuellInntektGrunnlag(
            manuelleInntekter = hentManuellInntektVurderinger(manuellInntektVurderingId)
        )
    }

    override fun hentHistoriskeVurderinger(
        sakId: SakId,
        behandlingId: BehandlingId,
        ekskluderteBehandlingIdListe: List<BehandlingId>
    ): List<ManuellInntektVurdering> {
        val harEkskludering = ekskluderteBehandlingIdListe.isNotEmpty()
        var query = """
            SELECT MANUELL_INNTEKT_VURDERINGER_ID
            FROM MANUELL_INNTEKT_VURDERING_GRUNNLAG GRUNNLAG
                JOIN BEHANDLING B1 ON B1.ID = GRUNNLAG.BEHANDLING_ID
            WHERE GRUNNLAG.AKTIV
            AND B1.SAK_ID = ?
            AND B1.OPPRETTET_TID < (SELECT B2.OPPRETTET_TID FROM BEHANDLING B2 WHERE ID = ?)
        """.trimIndent()

        if (harEkskludering) {
            query = "$query AND B1.ID <> ALL(?::bigint[])"
        }

        return connection.querySet(query) {
            setParams {
                setLong(1, sakId.id)
                setLong(2, behandlingId.id)
                if (harEkskludering) {
                    setLongArray(3, ekskluderteBehandlingIdListe.map { it.toLong() })
                }
            }
            setRowMapper {
                hentManuellInntektVurderinger(it.getLong("MANUELL_INNTEKT_VURDERINGER_ID"))
            }
        }.flatten()
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
                    belop = it.getBigDecimal("belop").let(::Beløp),
                    vurdertAv = it.getString("vurdert_av")
                )
            }
        }
    }

    override fun lagre(behandlingId: BehandlingId, manuellVurdering: ManuellInntektVurdering) {
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)

        // Hvis det finnes en vurdering på samme år, så overskrives denne
        val kombinerteEntries = (eksisterendeGrunnlag?.manuelleInntekter.orEmpty()
            .filterNot { it.år == manuellVurdering.år } + manuellVurdering).toSet()

        lagre(behandlingId, kombinerteEntries)
    }

    override fun lagre(behandlingId: BehandlingId, manuellVurderinger: Set<ManuellInntektVurdering>) {
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)
        if (eksisterendeGrunnlag != null) {
            deaktiverEksisterende(behandlingId)
        }

        val manuellInntektVurderingerQuery = """
            INSERT INTO MANUELL_INNTEKT_VURDERINGER DEFAULT VALUES
        """.trimIndent()
        val manuellInntektVurderingerId =
            connection.executeReturnKey(manuellInntektVurderingerQuery)

        lagreVurdering(manuellVurderinger, manuellInntektVurderingerId)

        val grunnlagQuery = """
            INSERT INTO MANUELL_INNTEKT_VURDERING_GRUNNLAG (BEHANDLING_ID, MANUELL_INNTEKT_VURDERINGER_ID) VALUES (?, ?)
        """.trimIndent()
        connection.execute(grunnlagQuery) {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, manuellInntektVurderingerId)
            }
        }
    }

    private fun lagreVurdering(
        manuellVurderinger: Set<ManuellInntektVurdering>,
        manuellInntektVurderingerId: Long
    ) {
        val query = """
            INSERT INTO MANUELL_INNTEKT_VURDERING (AR, BEGRUNNELSE, BELOP, VURDERT_AV, MANUELL_INNTEKT_VURDERINGER_ID) VALUES (?, ?, ?, ?, ?)
        """.trimIndent()

        connection.executeBatch(query, manuellVurderinger) {
            setParams {
                setInt(1, it.år.value)
                setString(2, it.begrunnelse)
                setBigDecimal(3, it.belop.verdi)
                setString(4, it.vurdertAv)
                setLong(5, manuellInntektVurderingerId)
            }
        }
    }

    private fun hentVurderingerIds(behandlingId: BehandlingId): List<Long> = connection.queryList(
        """
            SELECT MANUELL_INNTEKT_VURDERINGER_ID
            FROM MANUELL_INNTEKT_VURDERING_GRUNNLAG
            WHERE behandling_id = ?
         """.trimIndent()
    ) {
        setParams { setLong(1, behandlingId.id) }
        setRowMapper { row ->
            row.getLong("MANUELL_INNTEKT_VURDERINGER_ID")
        }
    }

    override fun slett(behandlingId: BehandlingId) {
        val manuellInntektVurderingerIds = hentVurderingerIds(behandlingId)

        // TODO: burde ikke det slettes rader fra MANUELL_INNTEKT_VURDERINGER også?
        val deletedRows = connection.executeReturnUpdated(
            """
            delete from MANUELL_INNTEKT_VURDERING_GRUNNLAG where behandling_id = ?;
            delete from MANUELL_INNTEKT_VURDERING where id = ANY(?::bigint[]);
        """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.id)
                setLongArray(2, manuellInntektVurderingerIds)
            }
        }
        log.info("Slettet $deletedRows rader fra MANUELL_INNTEKT_VURDERING_GRUNNLAG")
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
                (behandling_id, MANUELL_INNTEKT_VURDERINGER_ID) 
            SELECT ?, MANUELL_INNTEKT_VURDERINGER_ID
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
