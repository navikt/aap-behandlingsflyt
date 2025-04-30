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

    companion object : Factory<SamordningAndreStatligeYtelserRepositoryImpl> {
        override fun konstruer(connection: DBConnection): SamordningAndreStatligeYtelserRepositoryImpl {
            return SamordningAndreStatligeYtelserRepositoryImpl(connection)
        }
    }

    private val log = LoggerFactory.getLogger(javaClass)

    override fun hentHvisEksisterer(behandlingId: BehandlingId): SamordningAndreStatligeYtelserGrunnlag? {
        val query = """
            SELECT * FROM SAMORDNING_ANDRE_STATLIGE_YTELSER_GRUNNLAG WHERE behandling_id = ? and aktiv = true
        """.trimIndent()
        return connection.queryFirstOrNull(query) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper {
                val begrunnelseOgVurdertAv = hentSamordningAndreStatligeYtelserVurderingBegrunnelseOgVurdertAv(it.getLong("vurdering_id"))
                SamordningAndreStatligeYtelserGrunnlag(
                    vurdering = SamordningAndreStatligeYtelserVurdering(
                        begrunnelse = begrunnelseOgVurdertAv.begrunnelse,
                        vurdertAv = begrunnelseOgVurdertAv.vurdertAv,
                        vurderingPerioder = hentSamordningAndreStatligeYtelserVurderingPerioder(it.getLong("vurdering_id"))
                    )
                )
            }
        }
    }

    private data class BegrunnelseOgVurdertAv(
        val begrunnelse: String,
        val vurdertAv: String,
    )

    private fun hentSamordningAndreStatligeYtelserVurderingBegrunnelseOgVurdertAv(vurderingId: Long): BegrunnelseOgVurdertAv {
        val query = """
            SELECT * FROM SAMORDNING_ANDRE_STATLIGE_YTELSER_VURDERING WHERE id = ?
        """.trimIndent()
        return connection.queryFirst(query) {
            setParams {
                setLong(1, vurderingId)
            }
            setRowMapper {
                BegrunnelseOgVurdertAv(
                    begrunnelse = it.getString("begrunnelse"),
                    vurdertAv = it.getString("vurdert_av"),
                )

            }
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
                    beløp = it.getInt("belop"),
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
        // Ikke relevant for trukkede søknader, da man ikke vil ha fått meldeperioder
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
