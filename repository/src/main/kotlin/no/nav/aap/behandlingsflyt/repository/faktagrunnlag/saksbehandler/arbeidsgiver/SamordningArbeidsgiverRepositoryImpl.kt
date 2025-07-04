package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.arbeidsgiver

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.arbeidsgiver.SamordningArbeidsgiverGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.arbeidsgiver.SamordningArbeidsgiverRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.arbeidsgiver.SamordningArbeidsgiverVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.Factory
import org.slf4j.LoggerFactory
import kotlin.jvm.javaClass

class SamordningArbeidsgiverRepositoryImpl(private val connection: DBConnection) : SamordningArbeidsgiverRepository {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object : Factory<SamordningArbeidsgiverRepositoryImpl> {
        override fun konstruer(connection: DBConnection): SamordningArbeidsgiverRepositoryImpl {
            return SamordningArbeidsgiverRepositoryImpl(connection)
        }
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): SamordningArbeidsgiverGrunnlag? {

        val vurderingId = hentVurderingIdForBehandling(behandlingId) ?: return null
        val query = """
        SELECT * FROM SAMORDNING_ARBEIDSGIVER_VURDERING WHERE behandling_id = ? AND aktiv = true
    """.trimIndent()

        return connection.queryFirst(query) {
            setParams {
                setLong(1, vurderingId)
            }
            setRowMapper {
                SamordningArbeidsgiverGrunnlag(
                    vurdering = SamordningArbeidsgiverVurdering(
                        begrunnelse = it.getString("begrunnelse"),
                        vurdertAv = it.getString("vurdert_av"),
                        fom = it.getLocalDateOrNull("fom"),
                        tom = it.getLocalDateOrNull("tom"),
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

    override fun lagre(sakId: SakId, behandlingId: BehandlingId, samordningVurderinger: SamordningArbeidsgiverVurdering) {
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)
        if (eksisterendeGrunnlag != null) {
            deaktiverEksisterende(behandlingId)
        }
        val vurderingId = connection.executeReturnKey(
            """
            INSERT INTO SAMORDNING_ARBEIDSGIVER_VURDERING DEFAULT VALUES
        """.trimIndent()
        )

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

    override fun slett(behandlingId: BehandlingId) {
        val samordningArbeidsgiverVurderingIds = getSamordningArbeidsgiverVurderingIds(behandlingId)

        val deletedRows = connection.executeReturnUpdated(
            """
            delete from SAMORDNING_ARBEIDSGIVER_GRUNNLAG where behandling_id = ?;
            delete from SAMORDNING_ARBEIDSGIVER_VURDERING where id = ANY(?::bigint[]);
 
            
        """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.id)
                setLongArray(2, samordningArbeidsgiverVurderingIds)
            }
        }
        log.info("Slettet $deletedRows rader fra SAMORDNING_ARBEIDSGIVER_GRUNNLAG")
    }

    private fun getSamordningArbeidsgiverVurderingIds(behandlingId: BehandlingId): List<Long> = connection.queryList(
        """
                    SELECT samordning_arbeidsgiver_vurdering_id
                    FROM SAMORDNING_ARBEIDSGIVER_GRUNNLAG
                    WHERE behandling_id = ?
                      AND samordning_arbeidsgiver_vurdering_id is not null
                 
                """.trimIndent()
    ) {
        setParams { setLong(1, behandlingId.id) }
        setRowMapper { row ->
            row.getLong("samordning_arbeidsgiver_vurdering_id")
        }
    }

    private fun lagreVurdering(vurdering: SamordningArbeidsgiverVurdering, vurderingerId: Long): Long {
        val query = """
            INSERT INTO SAMORDNING_ARBEIDSGIVER_VURDERING (BEGRUNNELSE, FOM, TOM, VURDERT_AV, SAMORDNING_ARBEIDSGIVER_VURDERINGER_ID) VALUES (?, ?, ?, ?, ?)
        """.trimIndent()

        return connection.executeReturnKey(query) {
            setParams {
                setString(1, vurdering.begrunnelse)
                setLocalDate(2, vurdering.fom)
                setLocalDate(3, vurdering.tom)
                setString(4, vurdering.vurdertAv)
                setLong(5, vurderingerId)
            }
        }
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