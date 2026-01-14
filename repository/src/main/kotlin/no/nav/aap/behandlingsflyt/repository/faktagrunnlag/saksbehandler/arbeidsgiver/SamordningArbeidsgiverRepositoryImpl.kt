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

        //Denne skal sjekke om vurdereringer som er knyttet til denne behandling er knyttet til andre behandlinger også

        vurderingIder.isEmpty() -> emptyList()
        else -> {
            val sql = """
            SELECT SAMORDNING_ARBEIDSGIVER_VURDERING_ID
            FROM SAMORDNING_ARBEIDSGIVER_GRUNNLAG
            WHERE SAMORDNING_ARBEIDSGIVER_VURDERING_ID = ANY(?::bigint[])
            GROUP BY SAMORDNING_ARBEIDSGIVER_VURDERING_ID
            HAVING COUNT(*) = 1
        """.trimIndent()

            connection.queryList(sql) {
                setParams {
                    setLongArray(1, vurderingIder)
                }
                setRowMapper { rs ->
                    rs.getLong("SAMORDNING_ARBEIDSGIVER_VURDERING_ID")
                }
            }
        }
    }

    override fun slett(behandlingId: BehandlingId) {

        // Hent alle vurdering IDs knyttet til behandling
        val samordningArbeidsgiverVurderingIds = getSamordningArbeidsgiverVurderingIds(behandlingId)
        // Finn vurdering IDs som kun har ett grunnlag (denne behandlingen)
        val vurderingsIdMedKunEttGrunnlag = finnVurderingerMedKunEtGrunnlag(samordningArbeidsgiverVurderingIds)

        val deletedRowsGrunnlag = connection.executeReturnUpdated(
            """
            delete from SAMORDNING_ARBEIDSGIVER_GRUNNLAG where behandling_id = ?
        """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.id)
            }
        }
        log.info("Slettet $deletedRowsGrunnlag rader fra SAMORDNING_ARBEIDSGIVER_GRUNNLAG")

        if (vurderingsIdMedKunEttGrunnlag.isNotEmpty()) {

            connection.execute(
                """
                delete from SAMORDNING_ARBEIDSGIVER_VURDERING_PERIODE where VURDERING_ID = ANY(?::bigint[])
            """.trimIndent()
            ) {
                setParams {
                    setLongArray(1, vurderingsIdMedKunEttGrunnlag)
                }
            }
            val deletedRowsVurdering = connection.executeReturnUpdated(
                """
                delete from SAMORDNING_ARBEIDSGIVER_VURDERING where id = ANY(?::bigint[])
            """.trimIndent()
            ) {
                setParams {
                    setLongArray(1, vurderingsIdMedKunEttGrunnlag)
                }
            }
            log.info("Slettet $deletedRowsVurdering rader fra SAMORDNING_ARBEIDSGIVER_VURDERING")
        }


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