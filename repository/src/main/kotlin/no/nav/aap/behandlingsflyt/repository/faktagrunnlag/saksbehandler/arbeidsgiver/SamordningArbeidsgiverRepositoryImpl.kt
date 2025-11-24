package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.arbeidsgiver

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.SamordningAndreStatligeYtelserVurderingPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.arbeidsgiver.SamordningArbeidsgiverGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.arbeidsgiver.SamordningArbeidsgiverRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.arbeidsgiver.SamordningArbeidsgiverVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.lookup.repository.Factory
import org.slf4j.LoggerFactory

class SamordningArbeidsgiverRepositoryImpl(private val connection: DBConnection) : SamordningArbeidsgiverRepository {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object : Factory<SamordningArbeidsgiverRepositoryImpl> {
        override fun konstruer(connection: DBConnection): SamordningArbeidsgiverRepositoryImpl {
            return SamordningArbeidsgiverRepositoryImpl(connection)
        }
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): SamordningArbeidsgiverGrunnlag? {

        val vurderingId = hentVurderingIdForBehandling(behandlingId) ?: return null


        //finn perioder
        val perioderQuery = """
            select FOM, TOM from SAMORDNING_ARBEIDSGIVER_VURDERING_PERIODE where VURDERING_ID = ?
        """.trimIndent()

        val periodeListe = connection.queryList(perioderQuery) {
            setParams {
                setLong(1, vurderingId)
            }
            setRowMapper { row ->
                Periode(
                    fom = row.getLocalDate("FOM"),
                    tom = row.getLocalDate("TOM")
                )
            }
        }



        val query = """
        SELECT * FROM SAMORDNING_ARBEIDSGIVER_VURDERING WHERE id = ?
    """.trimIndent()

         return connection.queryFirst(query) {
            setParams {
                setLong(1, vurderingId)
            }
            setRowMapper {

                val fom = it.getLocalDateOrNull("FOM")
                val tom = it.getLocalDateOrNull("TOM")
                val periodeFraFørPeriodisering = if (fom != null && tom != null) Periode(fom, tom) else null
                val allePerioder = periodeListe.toMutableList().apply {
                    periodeFraFørPeriodisering?.let { add(it) }
                }
                SamordningArbeidsgiverGrunnlag(
                    vurdering = SamordningArbeidsgiverVurdering(
                        begrunnelse = it.getString("begrunnelse"),
                        vurdertAv = it.getString("vurdert_av"),
                        perioder = allePerioder,
                        vurdertTidspunkt = it.getLocalDateTime("opprettet_tid"),
                    )
                )
            }
        }



    }





    private fun hentVurderingIdForBehandling(behandlingId: BehandlingId): Long? {
        val query = """
            SELECT samordning_arbeidsgiver_vurdering_id FROM SAMORDNING_ARBEIDSGIVER_GRUNNLAG WHERE behandling_id = ? AND aktiv = true
        """.trimIndent()
        return connection.queryFirstOrNull(query) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper { it.getLong("samordning_arbeidsgiver_vurdering_id") }
        }
    }

    override fun lagre(
        sakId: SakId,
        behandlingId: BehandlingId,
        refusjonkravVurderinger: SamordningArbeidsgiverVurdering
    ) {
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)
        if (eksisterendeGrunnlag != null) {
            deaktiverEksisterende(behandlingId)
        }
        val vurderingId = lagreVurdering(refusjonkravVurderinger)

        val grunnlagQuery = """
            INSERT INTO SAMORDNING_ARBEIDSGIVER_GRUNNLAG (BEHANDLING_ID, SAK_ID, SAMORDNING_ARBEIDSGIVER_VURDERING_ID) VALUES (?, ?, ?)
        """.trimIndent()
        connection.execute(grunnlagQuery) {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, sakId.id)
                setLong(3, vurderingId)
            }
        }
    }


    private fun finnVurderingerMedKunEtGrunnlag(vurderingIder: List<Long>): List<Long> = when {
        vurderingIder.isEmpty() -> emptyList()
        else -> {
            val placeholders = List(vurderingIder.size) { "?" }.joinToString(", ")
            val sql = """
            SELECT SAMORDNING_ARBEIDSGIVER_VURDERING_ID
            FROM SAMORDNING_ARBEIDSGIVER_GRUNNLAG
            WHERE SAMORDNING_ARBEIDSGIVER_VURDERING_ID IN ($placeholders)
            GROUP BY SAMORDNING_ARBEIDSGIVER_VURDERING_ID
            HAVING COUNT(*) = 1
        """.trimIndent()

            connection.queryList(sql) {
                setParams {
                    vurderingIder.forEachIndexed { i, id -> setLong(i + 1, id) }
                }
                setRowMapper { rs ->
                    rs.getLong("SAMORDNING_ARBEIDSGIVER_VURDERING_ID")
                }
            }
        }
    }

    override fun slett(behandlingId: BehandlingId) {

        val samordningArbeidsgiverVurderingIds = getSamordningArbeidsgiverVurderingIds(behandlingId)
        val vuderingsIdMedKunEttGrunnlag =  finnVurderingerMedKunEtGrunnlag(samordningArbeidsgiverVurderingIds)

        val deletedRows = connection.executeReturnUpdated(
            """
            delete from SAMORDNING_ARBEIDSGIVER_VURDERING where id = ANY(?::bigint[]);
            delete from SAMORDNING_ARBEIDSGIVER_VURDERING_PERIODE where VURDERING_ID = ANY(?::bigint[]);

        """.trimIndent()
        ) {
            setParams {
                setLongArray(1, vuderingsIdMedKunEttGrunnlag)
                setLongArray(2, vuderingsIdMedKunEttGrunnlag)
            }
        }
        log.info("Slettet $deletedRows rader fra SAMORDNING_ARBEIDSGIVER_VURDERING")

        val deletedRowsGrunnlag = connection.executeReturnUpdated(
            """
            delete from SAMORDNING_ARBEIDSGIVER_GRUNNLAG where behandling_id = ?;
 
            
        """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.id) }
        }
        log.info("Slettet $deletedRowsGrunnlag rader fra SAMORDNING_ARBEIDSGIVER_GRUNNLAG")
    }

    private fun getSamordningArbeidsgiverVurderingIds(behandlingId: BehandlingId): List<Long> = connection.queryList(
        """
                    SELECT samordning_arbeidsgiver_vurdering_id
                    FROM SAMORDNING_ARBEIDSGIVER_GRUNNLAG
                    WHERE behandling_id = ?
                """.trimIndent()
    ) {
        setParams { setLong(1, behandlingId.id) }
        setRowMapper { row ->
            row.getLong("samordning_arbeidsgiver_vurdering_id")
        }
    }



    private fun lagreVurdering(vurdering: SamordningArbeidsgiverVurdering): Long {


        val query = """
            INSERT INTO SAMORDNING_ARBEIDSGIVER_VURDERING (BEGRUNNELSE, VURDERT_AV) VALUES (?, ?)
        """.trimIndent()

        val vunderingsId = connection.executeReturnKey(query) {
            setParams {
                setString(1, vurdering.begrunnelse)
                setString(2, vurdering.vurdertAv)
            }
        }

        val periodeQuery = """
            INSERT INTO SAMORDNING_ARBEIDSGIVER_VURDERING_PERIODE (VURDERING_ID, FOM, TOM) VALUES (?, ?, ?)
        """.trimIndent()

        connection.executeBatch(periodeQuery,vurdering.perioder) {
            setParams {
                setLong(1,vunderingsId)
                setLocalDate(2,it.fom)
                setLocalDate(3,it.tom)
            }
        }

        return vunderingsId
    }

    private fun deaktiverEksisterende(behandlingId: BehandlingId) {
        connection.execute("UPDATE SAMORDNING_ARBEIDSGIVER_GRUNNLAG SET AKTIV = FALSE WHERE AKTIV AND BEHANDLING_ID = ?") {
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
            INSERT INTO SAMORDNING_ARBEIDSGIVER_GRUNNLAG 
                (behandling_id, sak_id, samordning_arbeidsgiver_vurdering_id) 
            SELECT ?, sak_id, samordning_arbeidsgiver_vurdering_id
                from SAMORDNING_ARBEIDSGIVER_GRUNNLAG 
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