package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.samordning

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.SamordningAndreStatligeYtelserGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.SamordningAndreStatligeYtelserRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.SamordningAndreStatligeYtelserVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.SamordningAndreStatligeYtelserVurderingPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.Factory
import org.slf4j.LoggerFactory

class SamordningAndreStatligeYtelserRepositoryImpl(private val connection: DBConnection) : SamordningAndreStatligeYtelserRepository {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object : Factory<SamordningAndreStatligeYtelserRepositoryImpl> {
        override fun konstruer(connection: DBConnection): SamordningAndreStatligeYtelserRepositoryImpl {
            return SamordningAndreStatligeYtelserRepositoryImpl(connection)
        }
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): SamordningAndreStatligeYtelserGrunnlag? {
        val vurderingId = hentVurderingIdForBehandling(behandlingId) ?: return null

        val vurderingPerioder = hentSamordningAndreStatligeYtelserVurderingPerioder(vurderingId)

        val query = """
            SELECT * FROM SAMORDNING_ANDRE_STATLIGE_YTELSER_VURDERING WHERE id = ?
        """.trimIndent()
        return connection.queryFirst(query) {
            setParams {
                setLong(1, vurderingId)
            }
            setRowMapper {
                SamordningAndreStatligeYtelserGrunnlag(
                    vurdering = SamordningAndreStatligeYtelserVurdering(
                        begrunnelse = it.getString("begrunnelse"),
                        vurdertAv = it.getString("vurdert_av"),
                        vurdertTidspunkt = it.getLocalDateTime("opprettet_tid"),
                        vurderingPerioder = vurderingPerioder
                    )
                )
            }
        }
    }

    private fun hentVurderingIdForBehandling(behandlingId: BehandlingId): Long? {
        val query = """
            SELECT vurdering_id FROM SAMORDNING_ANDRE_STATLIGE_YTELSER_GRUNNLAG WHERE behandling_id = ? AND aktiv = true
        """.trimIndent()
        return connection.queryFirstOrNull(query) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper { it.getLong("vurdering_id") }
        }
    }

    private fun hentSamordningAndreStatligeYtelserVurderingPerioder(vurderingId: Long): List<SamordningAndreStatligeYtelserVurderingPeriode> {
        val query = """
            SELECT * FROM SAMORDNING_ANDRE_STATLIGE_YTELSER_VURDERING_PERIODE WHERE vurdering_id = ?
        """.trimIndent()
        return connection.queryList(query) {
            setParams {
                setLong(1, vurderingId)
            }
            setRowMapper {
                SamordningAndreStatligeYtelserVurderingPeriode(
                    periode = it.getPeriode("periode"),
                    ytelse = it.getEnum("ytelse_type"),
                    beløp = it.getInt("belop")
                )
            }
        }
    }

    override fun lagre(behandlingId: BehandlingId, vurdering: SamordningAndreStatligeYtelserVurdering) {
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)
        if (eksisterendeGrunnlag != null) {
            deaktiverGrunnlag(behandlingId)
        }

        val samordingAndreStatligeYtelserVurderingQuery = """
            INSERT INTO SAMORDNING_ANDRE_STATLIGE_YTELSER_VURDERING (begrunnelse, vurdert_av) VALUES (?, ?)
        """.trimIndent()
        val vurderingId = connection.executeReturnKey(samordingAndreStatligeYtelserVurderingQuery) {
            setParams {
                setString(1, vurdering.begrunnelse)
                setString(2, vurdering.vurdertAv)
            }
        }

        val samordingUføreGrunnlagQuery = """
            INSERT INTO SAMORDNING_ANDRE_STATLIGE_YTELSER_GRUNNLAG (behandling_id, vurdering_id) VALUES (?, ?)
        """.trimIndent()
        connection.execute(samordingUføreGrunnlagQuery) {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, vurderingId)
            }
        }

        val periodeQuery = """
                INSERT INTO SAMORDNING_ANDRE_STATLIGE_YTELSER_VURDERING_PERIODE (vurdering_id, periode, ytelse_type, belop) VALUES (?, ?::daterange, ?, ?)
                """.trimIndent()
        connection.executeBatch(periodeQuery, vurdering.vurderingPerioder) {
            setParams {
                setLong(1, vurderingId)
                setPeriode(2, it.periode)
                setEnumName(3, it.ytelse)
                setInt(4, it.beløp)
            }
        }
    }

    override fun slett(behandlingId: BehandlingId) {

        val samordningStatligYtelseVurderingIds = getSamordningStatligYtelseVurderingIds(behandlingId)

        val deletedRows = connection.executeReturnUpdated("""
            delete from samordning_andre_statlige_ytelser_grunnlag where behandling_id = ?; 
            delete from samordning_andre_statlige_ytelser_vurdering_periode where vurdering_id = ANY(?::bigint[]);
            delete from samordning_andre_statlige_ytelser_vurdering where id = ANY(?::bigint[]);
           
        """.trimIndent()) {
            setParams {
                setLong(1, behandlingId.id)
                setLongArray(2, samordningStatligYtelseVurderingIds)
                setLongArray(3, samordningStatligYtelseVurderingIds)
            }
        }
        log.info("Slettet $deletedRows rader fra samordning_andre_statlige_ytelser_grunnlag")
    }

    private fun getSamordningStatligYtelseVurderingIds(behandlingId: BehandlingId): List<Long> = connection.queryList(
        """
                    SELECT vurdering_id
                    FROM samordning_andre_statlige_ytelser_grunnlag
                    WHERE behandling_id = ? AND vurdering_id is not null
                 
                """.trimIndent()
    ) {
        setParams { setLong(1, behandlingId.id) }
        setRowMapper { row ->
            row.getLong("vurdering_id")
        }
    }

    private fun deaktiverGrunnlag(behandlingId: BehandlingId) {
        connection.execute("UPDATE SAMORDNING_ANDRE_STATLIGE_YTELSER_GRUNNLAG set aktiv = false WHERE behandling_id = ? and aktiv = true") {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setResultValidator { require(it == 1) }
        }
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        val eksisterer = hentHvisEksisterer(fraBehandling)
        if (eksisterer == null) {
            return
        }
        val query = """
            INSERT INTO SAMORDNING_ANDRE_STATLIGE_YTELSER_GRUNNLAG (behandling_id, vurdering_id) SELECT ?, vurdering_id from SAMORDNING_ANDRE_STATLIGE_YTELSER_GRUNNLAG where behandling_id = ? and aktiv
        """.trimIndent()

        connection.execute(query) {
            setParams {
                setLong(1, tilBehandling.toLong())
                setLong(2, fraBehandling.toLong())
            }
        }
    }
}
