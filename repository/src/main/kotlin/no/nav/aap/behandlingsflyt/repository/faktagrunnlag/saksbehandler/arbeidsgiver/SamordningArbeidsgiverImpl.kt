package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.arbeidsgiver

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsgiver.SamordningArbeidsgiverRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsgiver.SamordningArbeidsgiverVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.Factory
import org.slf4j.LoggerFactory
import kotlin.jvm.javaClass

class SamordningArbeidsgiverImpl(private val connection: DBConnection) : SamordningArbeidsgiverRepository {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object : Factory<SamordningArbeidsgiverImpl> {
        override fun konstruer(connection: DBConnection): SamordningArbeidsgiverImpl {
            return SamordningArbeidsgiverImpl(connection)
        }
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): SamordningArbeidsgiverVurdering? {
        val query = """
        SELECT * FROM SAMORDNING_ARBEIDSGIVER_GRUNNLAG WHERE behandling_id = ? AND aktiv = true
    """.trimIndent()

        val vurderingerId = connection.queryFirstOrNull(query) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper {
                it.getLong("samordning_arbeidsgiver_vurdering_id")
            }
        }

        return vurderingerId?.let { hentArbeidsgiverSAMORDNING(it) }
    }

    private fun hentArbeidsgiverSAMORDNING(vurderingerId: Long): List<SamordningArbeidsgiverVurdering> {
        val query = """
            SELECT * FROM SAMORDNING_ARBEIDSGIVER_VURDERING WHERE ID = ?
        """.trimIndent()

        return connection.queryList(query) {
            setParams {
                setLong(1, vurderingerId)
            }
            setRowMapper {
                SamordningArbeidsgiverVurdering(
                    fom = it.getLocalDateOrNull("fom"),
                    tom = it.getLocalDateOrNull("tom"),
                    vurdertAv = it.getString("vurdert_av"),
                    vurdering = it.getString("vurdering"),
                    opprettetTid = it.getLocalDateTime("opprettet_tid")
                )
            }
        }
    }

    override fun hentAlleVurderingerPåSak(sakId: SakId): List<SamordningArbeidsgiverVurdering> {
        val query = """
            SELECT * FROM SAMORDNING_ARBEIDSGIVER_GRUNNLAG WHERE sak_id = ? and aktiv = true
        """.trimIndent()

        return connection.queryList(query) {
            setParams {
                setLong(1, sakId.id)
            }
            setRowMapper {
                hentArbeidsgiverSAMORDNING(it.getLong("SAMORDNING_ARBEIDSGIVER_VURDERING_ID"))
            }
        }.flatten()
    }

    override fun lagre(sakId: SakId, behandlingId: BehandlingId, SAMORDNINGVurderinger: List<SamordningArbeidsgiverVurdering>) {
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
        val refusjonskravArbeidsgiverVurderingIds = getRefusjonskravArbeidsgiverVurderingIds(behandlingId)

        val deletedRows = connection.executeReturnUpdated(
            """
            delete from SAMORDNING_ARBEIDSGIVER_GRUNNLAG where behandling_id = ?;
            delete from SAMORDNING_ARBEIDSGIVER_VURDERING where SAMORDNING_arbeidsgiver_vurderinger_id = ANY(?::bigint[]);
 
            
        """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.id)
                setLongArray(2, refusjonskravArbeidsgiverVurderingIds)
            }
        }
        log.info("Slettet $deletedRows rader fra SAMORDNING_ARBEIDSGIVER_GRUNNLAG")
    }

    private fun getRefusjonskravArbeidsgiverVurderingIds(behandlingId: BehandlingId): List<Long> = connection.queryList(
        """
                    SELECT SAMORDNING_arbeidsgiver_vurdering_id
                    FROM SAMORDNING_ARBEIDSGIVER_GRUNNLAG
                    WHERE behandling_id = ?
                      AND SAMORDNING_arbeidsgiver_vurdering_id is not null
                 
                """.trimIndent()
    ) {
        setParams { setLong(1, behandlingId.id) }
        setRowMapper { row ->
            row.getLong("SAMORDNING_arbeidsgiver_vurdering_id")
        }
    }

    private fun lagreVurdering(vurdering: SamordningArbeidsgiverVurdering, vurderingerId: Long): Long {
        val query = """
            INSERT INTO SAMORDNING_ARBEIDSGIVER_VURDERING (VURDERING, FOM, TOM, VURDERT_AV, SAMORDNING_ARBEIDSGIVER_VURDERINGER_ID) VALUES (?, ?, ?, ?, ?)
        """.trimIndent()

        return connection.executeReturnKey(query) {
            setParams {
                setString(1, vurdering.vurdering)
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
                (behandling_id, sak_id, SAMORDNING_arbeidsgiver_vurdering_id) 
            SELECT ?, sak_id, SAMORDNING_arbeidsgiver_vurdering_id
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