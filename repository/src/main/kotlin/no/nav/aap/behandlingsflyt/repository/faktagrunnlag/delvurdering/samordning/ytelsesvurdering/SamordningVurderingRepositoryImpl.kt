package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.samordning.ytelsesvurdering

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurderingGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurderingPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningVurderingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.lookup.repository.Factory
import org.slf4j.LoggerFactory

class SamordningVurderingRepositoryImpl(private val connection: DBConnection) :
    SamordningVurderingRepository {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object : Factory<SamordningVurderingRepositoryImpl> {
        override fun konstruer(connection: DBConnection): SamordningVurderingRepositoryImpl {
            return SamordningVurderingRepositoryImpl(connection)
        }
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): SamordningVurderingGrunnlag? {
        val query = """
            SELECT * FROM SAMORDNING_YTELSEVURDERING_GRUNNLAG WHERE behandling_id = ? and aktiv = true
        """.trimIndent()
        return connection.queryFirstOrNull(query) {
            setParams {
                setLong(1, behandlingId.id)
            }
            setRowMapper {
                // At denne kunne være nullable er egentlig rester fra tidligere. Vurder å slette
                // grunnlag uten vurderinger?
                it.getLongOrNull("vurderinger_id")?.let(::hentSamordningVurderinger)
            }
        }
    }

    override fun hentHistoriskeVurderinger(
        sakId: SakId,
        behandlingId: BehandlingId
    ): List<SamordningVurderingGrunnlag> {
        val query = """
            SELECT VURDERINGER_ID
            FROM SAMORDNING_YTELSEVURDERING_GRUNNLAG GRUNNLAG 
                JOIN BEHANDLING B1 ON B1.ID = GRUNNLAG.BEHANDLING_ID
                LEFT JOIN AVBRYT_REVURDERING_GRUNNLAG AR ON AR.BEHANDLING_ID = B1.ID
            WHERE GRUNNLAG.AKTIV
            AND B1.SAK_ID = ?
            AND B1.OPPRETTET_TID < (SELECT B2.OPPRETTET_TID FROM BEHANDLING B2 WHERE B2.ID = ?)
            AND AR.BEHANDLING_ID IS NULL
        """.trimIndent()

        return connection.queryList(query) {
            setParams {
                setLong(1, sakId.id)
                setLong(2, behandlingId.id)
            }
            setRowMapper {
                hentSamordningVurderinger(it.getLong("vurderinger_id"))
            }
        }
    }

    private fun hentSamordningVurderinger(vurderingerId: Long): SamordningVurderingGrunnlag {
        val vurderingerQuery = """
            SELECT sv.id as sv_id, sv.ytelse_type as sv_ytelse_type
            FROM SAMORDNING_VURDERING sv
            WHERE sv.vurderinger_id = ?
        """.trimIndent()

        val vurderinger = connection.queryList(vurderingerQuery) {
            setParams {
                setLong(1, vurderingerId)
            }
            setRowMapper {
                SamordningVurdering(
                    ytelseType = it.getEnum("sv_ytelse_type"),
                    vurderingPerioder = hentSamordningVurderingPerioder(it.getLong("sv_id")),
                )
            }
        }.toSet()

        val fellesFelterSpørring = """
            SELECT begrunnelse, maksdato_endelig, frist_ny_revurdering, vurdert_av, opprettet_tid
            FROM SAMORDNING_VURDERINGER
            WHERE id = ?
        """.trimIndent()

        val vurderingGrunnlag =
            connection.queryFirst(fellesFelterSpørring) {
                setParams {
                    setLong(1, vurderingerId)
                }
                setRowMapper {
                    SamordningVurderingGrunnlag(
                        begrunnelse = it.getStringOrNull("begrunnelse"),
                        maksDatoEndelig = it.getBoolean("maksdato_endelig"),
                        fristNyRevurdering = it.getLocalDateOrNull("frist_ny_revurdering"),
                        vurderingerId = vurderingerId,
                        vurdertAv = it.getString("vurdert_av"),
                        vurdertTidspunkt = it.getLocalDateTime("opprettet_tid"),
                        vurderinger = vurderinger
                    )
                }
            }

        return vurderingGrunnlag
    }

    private fun hentSamordningVurderingPerioder(vurderingId: Long): Set<SamordningVurderingPeriode> {
        val query = """
            SELECT * FROM SAMORDNING_VURDERING_PERIODE WHERE vurdering_id = ?
        """.trimIndent()
        return connection.queryList(query) {
            setParams {
                setLong(1, vurderingId)
            }
            setRowMapper {
                SamordningVurderingPeriode(
                    periode = it.getPeriode("periode"),
                    gradering = it.getIntOrNull("gradering")?.let(::Prosent),
                    kronesum = it.getIntOrNull("kronesum"),
                    manuell = it.getBooleanOrNull("manuell"),
                )
            }
        }.toSet()
    }


    override fun lagreVurderinger(
        behandlingId: BehandlingId,
        samordningVurderinger: SamordningVurderingGrunnlag
    ) {
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)
        if (eksisterendeGrunnlag != null) {
            deaktiverGrunnlag(behandlingId)
        }

        val samordningVurderingerQuery = """
            INSERT INTO SAMORDNING_VURDERINGER (begrunnelse, maksdato_endelig, frist_ny_revurdering, vurdert_av)
            VALUES (?, ?, ?, ?)
            """.trimIndent()
        val vurderingerId = connection.executeReturnKey(samordningVurderingerQuery) {
            setParams {
                setString(1, samordningVurderinger.begrunnelse)
                setBoolean(2, samordningVurderinger.maksDatoEndelig)
                setLocalDate(3, samordningVurderinger.fristNyRevurdering)
                setString(4, samordningVurderinger.vurdertAv)
            }
        }

        for (vurdering in samordningVurderinger.vurderinger) {
            val vurderingQuery = """
                INSERT INTO SAMORDNING_VURDERING (vurderinger_id, ytelse_type) VALUES (?, ?)
                """.trimIndent()
            val vurderingId = connection.executeReturnKey(vurderingQuery) {
                setParams {
                    setLong(1, vurderingerId)
                    setEnumName(2, vurdering.ytelseType)
                }
            }

            val veriodePeriodeQuery = """
                INSERT INTO SAMORDNING_VURDERING_PERIODE (periode, vurdering_id, gradering, kronesum, manuell)
                VALUES (?::daterange, ?, ?, ?, ?)
                """.trimIndent()
            connection.executeBatch(veriodePeriodeQuery, vurdering.vurderingPerioder) {
                setParams {
                    setPeriode(1, it.periode)
                    setLong(2, vurderingId)
                    setInt(3, it.gradering?.prosentverdi())
                    setInt(4, it.kronesum?.toInt())
                    setBoolean(5, it.manuell)
                }
            }
        }

        val grunnlagQuery = """
            INSERT INTO SAMORDNING_YTELSEVURDERING_GRUNNLAG (behandling_id, vurderinger_id) VALUES (?, ?)
        """.trimIndent()
        connection.execute(grunnlagQuery) {
            setParams {
                setLong(1, behandlingId.id)
                setLong(2, vurderingerId)
            }
        }
    }

    private fun deaktiverGrunnlag(behandlingId: BehandlingId) {
        connection.execute("UPDATE SAMORDNING_YTELSEVURDERING_GRUNNLAG set aktiv = false WHERE behandling_id = ? and aktiv = true") {
            setParams {
                setLong(1, behandlingId.id)
            }
            setResultValidator { require(it == 1) }
        }
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        hentHvisEksisterer(fraBehandling) ?: return

        val query = """
            INSERT INTO SAMORDNING_YTELSEVURDERING_GRUNNLAG 
                (behandling_id, vurderinger_id)
            SELECT ?, vurderinger_id
                from SAMORDNING_YTELSEVURDERING_GRUNNLAG 
                where behandling_id = ? and aktiv
        """.trimIndent()

        connection.execute(query) {
            setParams {
                setLong(1, tilBehandling.toLong())
                setLong(2, fraBehandling.toLong())
            }
        }
    }

    override fun slett(behandlingId: BehandlingId) {

        val samordningVurderingYtelseIds = getSamordningYtelseVurderingIds(behandlingId)
        val samordningVurderingIds = getSamordningVurderingIds(samordningVurderingYtelseIds)

        val deletedRows = connection.executeReturnUpdated(
            """
            delete from samordning_ytelsevurdering_grunnlag where behandling_id = ?; 
            delete from samordning_vurdering_periode where vurdering_id = ANY(?::bigint[]);
            delete from samordning_vurdering where vurderinger_id = ANY(?::bigint[]);
            delete from samordning_vurderinger where id = ANY(?::bigint[]);
           
        """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.id)
                setLongArray(2, samordningVurderingIds)
                setLongArray(3, samordningVurderingYtelseIds)
                setLongArray(4, samordningVurderingYtelseIds)
            }
        }
        log.info("Slettet $deletedRows rader fra samordning_ytelsevurdering_grunnlag")
    }

    private fun getSamordningYtelseVurderingIds(behandlingId: BehandlingId): List<Long> = connection.queryList(
        """
                    SELECT vurderinger_id
                    FROM samordning_ytelsevurdering_grunnlag
                    WHERE behandling_id = ? AND vurderinger_id is not null
                 
                """.trimIndent()
    ) {
        setParams { setLong(1, behandlingId.id) }
        setRowMapper { row ->
            row.getLong("vurderinger_id")
        }
    }

    private fun getSamordningVurderingIds(vurderingerIds: List<Long>): List<Long> = connection.queryList(
        """
                    SELECT id
                    FROM samordning_vurdering
                    WHERE vurderinger_id = ANY(?::bigint[]);
                """.trimIndent()
    ) {
        setParams { setLongArray(1, vurderingerIds) }
        setRowMapper { row ->
            row.getLong("id")
        }
    }
}
