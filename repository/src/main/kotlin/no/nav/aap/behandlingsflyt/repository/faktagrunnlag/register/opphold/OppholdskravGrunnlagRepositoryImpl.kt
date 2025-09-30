package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.register.opphold

import no.nav.aap.behandlingsflyt.behandling.oppholdskrav.OppholdskravGrunnlag
import no.nav.aap.behandlingsflyt.behandling.oppholdskrav.OppholdskravGrunnlagRepository
import no.nav.aap.behandlingsflyt.behandling.oppholdskrav.OppholdskravPeriode
import no.nav.aap.behandlingsflyt.behandling.oppholdskrav.OppholdskravVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.Factory
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime

class OppholdskravGrunnlagRepositoryImpl(private val connection: DBConnection) : OppholdskravGrunnlagRepository {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object : Factory<OppholdskravGrunnlagRepository> {
        override fun konstruer(connection: DBConnection): OppholdskravGrunnlagRepository {
            return OppholdskravGrunnlagRepositoryImpl(connection)
        }
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): OppholdskravGrunnlag? {

        val selectGrunnlag =
            "SELECT ID, OPPRETTET_TID  FROM OPPHOLDSKRAV_GRUNNLAG WHERE BEHANDLING_ID = ? AND AKTIV = TRUE"

        val grunnlangsIdOgDato = connection.queryFirstOrNull(selectGrunnlag) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper { row ->
                Pair(
                    row.getLong("ID"),
                    row.getLocalDateTime("OPPRETTET_TID")
                )
            }
        }

        if (grunnlangsIdOgDato == null) {
            return null
        }

        val (oppholdskraveneId, opprettetTid) = grunnlangsIdOgDato

        val selectOppholdskrav =
            """
                SELECT 
                periode.OPPFYLT,
                periode.LAND,
                periode.begrunnelse,
                periode.TOM,
                periode.FOM,
                vurdering.OPPRETTET_TID,
                vurdering.VURDERT_AV,
                vurdering.VURDERT_I_BEHANDLING,
                vurderinger.GRUNNLAG_ID,
                vurderinger.VURDERING_ID
                FROM OPPHOLDSKRAV_VURDERINGER vurderinger 
                INNER JOIN OPPHOLDSKRAV_VURDERING vurdering ON vurderinger.VURDERING_ID = vurdering.ID
                INNER JOIN OPPHOLDSKRAV_VURDERING_PERIODE periode ON vurdering.ID = periode.OPPHOLDSKRAV_VURDERING_ID
                WHERE vurderinger.GRUNNLAG_ID = ?
            """.trimIndent()


        val oppholdskravene = connection.queryList(selectOppholdskrav) {
            setParams {
                setLong(1, oppholdskraveneId)
            }
            setRowMapper { row ->
                OppholdskravInternal(
                    oppfylt = row.getBoolean("OPPFYLT"),
                    land = row.getStringOrNull("LAND"),
                    fom = row.getLocalDate("FOM"),
                    tom = row.getLocalDateOrNull("TOM"),
                    opprettet = row.getLocalDateTime("OPPRETTET_TID"),
                    vurdertAv = row.getString("VURDERT_AV"),
                    begrunnelse = row.getString("BEGRUNNELSE"),
                    grunnlagId = row.getLong("GRUNNLAG_ID"),
                    vurderingId = row.getLong("VURDERING_ID"),
                    vurdertIBehandling = row.getLong("VURDERT_I_BEHANDLING"),
                )
            }

        }

        return OppholdskravGrunnlag(
            opprettet = opprettetTid,
            vurderinger = oppholdskravene.groupBy { oppholdskravene -> oppholdskravene.vurderingId }
                .values
                .map { rader ->
                    OppholdskravVurdering(
                        opprettet = rader.first().opprettet,
                        vurdertAv = rader.first().vurdertAv,
                        vurdertIBehandling = BehandlingId(rader.first().vurdertIBehandling),
                        perioder = rader.map {
                            OppholdskravPeriode(
                                begrunnelse = it.begrunnelse,
                                fom = it.fom,
                                tom = it.tom,
                                land = it.land,
                                oppfylt = it.oppfylt,

                                )
                        }
                    )

                })
    }


    data class OppholdskravInternal(
        val grunnlagId: Long,
        val vurdertIBehandling: Long,
        val fom: LocalDate,
        val tom: LocalDate?,
        val opprettet: LocalDateTime,
        val vurdertAv: String,
        val begrunnelse: String,
        val land: String?,
        val oppfylt: Boolean,
        val vurderingId: Long
    )


    override fun lagre(
        behandlingId: BehandlingId,
        oppholdskravVurdering: OppholdskravVurdering
    ) {
        val eksisternedeGrunnlagId = hentGrunnlagsId(behandlingId)

        if (eksisternedeGrunnlagId != null) {
            deaktiverGrunnlag(behandlingId)
        }

        val grunnlagQuery = """
            INSERT INTO OPPHOLDSKRAV_GRUNNLAG (BEHANDLING_ID,AKTIV) VALUES (?, TRUE)
        """.trimIndent()

        val nyGrunnlagId = connection.executeReturnKey(grunnlagQuery) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
        }

        if (eksisternedeGrunnlagId != null) {
            connection.execute(
                """
                INSERT INTO OPPHOLDSKRAV_VURDERINGER (GRUNNLAG_ID, VURDERING_ID) (
                    SELECT ? AS GRUNNLAG_ID, vurdering.ID 
                    FROM OPPHOLDSKRAV_VURDERINGER vurderinger
                    INNER JOIN OPPHOLDSKRAV_VURDERING vurdering ON vurderinger.VURDERING_ID = vurdering.ID
                    WHERE vurdering.VURDERT_I_BEHANDLING != ? 
                    AND vurderinger.GRUNNLAG_ID = ?
                )
            """.trimIndent()
            ) {
                setParams {
                    setLong(1, nyGrunnlagId)
                    setLong(2, behandlingId.toLong())
                    setLong(3, eksisternedeGrunnlagId)
                }
            }
        }


        val nyVurderingId = connection.executeReturnKey(
            "INSERT INTO OPPHOLDSKRAV_VURDERING  (VURDERT_I_BEHANDLING,VURDERT_AV) values (?, ?)"
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
                setString(2, oppholdskravVurdering.vurdertAv)
            }
        }

        connection.executeBatch(
            """INSERT INTO OPPHOLDSKRAV_VURDERING_PERIODE  (
                OPPHOLDSKRAV_VURDERING_ID,
                FOM,
                TOM,
                BEGRUNNELSE,
                OPPFYLT,
                LAND
                ) values (?, ?, ?, ?, ?, ?)
                """, oppholdskravVurdering.perioder
        ) {
            setParams {
                setLong(1, nyVurderingId)
                setLocalDate(2, it.fom)
                setLocalDate(3, it.tom)
                setString(4, it.begrunnelse)
                setBoolean(5, it.oppfylt)
                setString(6, it.land)
            }
        }
        connection.execute("INSERT INTO OPPHOLDSKRAV_VURDERINGER ( GRUNNLAG_ID, VURDERING_ID) VALUES (?, ?)") {
            setParams {
                setLong(1, nyGrunnlagId)
                setLong(2, nyVurderingId)
            }
        }


    }

    private fun hentGrunnlagsId(behandlingsId: BehandlingId): Long? {
        val hentGrunnlagQuery = """
            SELECT ID FROM OPPHOLDSKRAV_GRUNNLAG WHERE behandling_id = ? AND AKTIV = TRUE
        """.trimIndent()

        return connection.queryFirstOrNull<Long>(hentGrunnlagQuery) {
            setParams {
                setLong(1, behandlingsId.toLong())
            }
            setRowMapper { it.getLong("ID") }
        }
    }

    override fun kopier(
        fraBehandling: BehandlingId,
        tilBehandling: BehandlingId
    ) {
        hentHvisEksisterer(fraBehandling) ?: return
        require(fraBehandling != tilBehandling)
        val grunnlagId =
            connection.executeReturnKey("INSERT INTO OPPHOLDSKRAV_GRUNNLAG(behandling_id, aktiv) VALUES(?, true)") {
                setParams { setLong(1, tilBehandling.toLong()) }
            }

        val queryKopierGamleVurderinger = """
            INSERT INTO OPPHOLDSKRAV_VURDERINGER (grunnlag_id, vurdering_id) (
                SELECT ?, vurdering.id
                FROM OPPHOLDSKRAV_GRUNNLAG grunnlag
                INNER JOIN OPPHOLDSKRAV_VURDERINGER vurderinger ON grunnlag.id = vurderinger.grunnlag_id
                INNER JOIN OPPHOLDSKRAV_VURDERING vurdering ON vurdering.id = vurderinger.vurdering_id
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

    override fun tilbakestillGrunnlag(behandlingId: BehandlingId, forrigeBehandling: BehandlingId?) {
        val eksisternedeGrunnlagId = hentGrunnlagsId(behandlingId)

        if (eksisternedeGrunnlagId != null) {
            deaktiverGrunnlag(behandlingId)
        }

        if (forrigeBehandling != null) {
            kopier(forrigeBehandling, behandlingId)
        }
    }

    override fun slett(behandlingId: BehandlingId) {

        /**
         * Finn grunnlag på behandling
         *
         * Finn vunderinger på Grunnlag
         *
         * hvis kun vunderinger peker på andre grunnlag må de vurderinger og perioder beholdes
         *
         * hvis ikke, kan vurderinger og perioder slettes
         */

        val grunnlagId = hentGrunnlagsId(behandlingId) ?: return
        val vurderinger = hentVurderingerIdForGrunnlag(grunnlagId)
        val vurderingerPåAnnetGrunnlag = sjekkOmDetFinnesVurderingerPåAnnentGrunnlag(vurderinger, grunnlagId)

        slettGrunnlagPåId(grunnlagId)

        if (vurderingerPåAnnetGrunnlag.isEmpty()){
            slettPeriodeOgVurderingPåVurderingID(vurderinger)
        }
    }

    private fun slettGrunnlagPåId(grunnlagId: Long){

        connection.execute("""
            DELETE FROM OPPHOLDSKRAV_VURDERINGER
            WHERE GRUNNLAG_ID = ?
            """.trimIndent()
        ) { setParams { setLong(1, grunnlagId) } }
        connection.execute(
            """
            DELETE FROM OPPHOLDSKRAV_GRUNNLAG
            WHERE ID = ?
            """.trimIndent()
        ) { setParams { setLong(1, grunnlagId) } }

    }


    private fun slettPeriodeOgVurderingPåVurderingID(vurderinger: List<Long>) {

            connection.execute("""
                    DELETE FROM OPPHOLDSKRAV_VURDERING_PERIODE
                    WHERE OPPHOLDSKRAV_VURDERING_ID = ANY (?)
                    """.trimIndent()
            ) { setParams { setLongArray(1, vurderinger) } }

            connection.execute("""
                    DELETE FROM OPPHOLDSKRAV_VURDERING
                    WHERE id = ANY (?)
                    """.trimIndent()
            ) { setParams { setLongArray(1, vurderinger) } }

    }


    private fun hentVurderingerIdForGrunnlag(grunnlagId: Long): List<Long> {
        val query = """ select VURDERING_ID from OPPHOLDSKRAV_VURDERINGER WHERE GRUNNLAG_ID = ? """

        return connection.queryList(query) {
            setParams {
                setLong(1, grunnlagId)
            }
            setRowMapper { it.getLong("VURDERING_ID") }
        }
    }

    private fun sjekkOmDetFinnesVurderingerPåAnnentGrunnlag(
        vurderingsIder: List<Long>,
        grunnlagId: Long
    ): List<Long> {
        if (vurderingsIder.isEmpty()) return emptyList()

        val placeholders = vurderingsIder.joinToString(",") { "?" }

        val query = """
        SELECT GRUNNLAG_ID 
        FROM OPPHOLDSKRAV_VURDERINGER 
        WHERE GRUNNLAG_ID != ? 
        AND VURDERING_ID IN ($placeholders)
    """

        return connection.queryList(query) {
            setParams {
                setLong(1, grunnlagId)
                vurderingsIder.forEachIndexed { index, id ->
                    setLong(index + 2, id)
                }
            }
            setRowMapper { it.getLong("GRUNNLAG_ID") }
        }
    }



    private fun deaktiverGrunnlag(behandlingId: BehandlingId) {
        connection.execute("UPDATE OPPHOLDSKRAV_GRUNNLAG SET aktiv = false WHERE behandling_id = ?") {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setResultValidator { require(it == 1) }
        }
    }


}