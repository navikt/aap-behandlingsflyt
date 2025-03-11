package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.samordning

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreVurderingPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.VurderingerForSamordningUføre
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.lookup.repository.Factory

class SamordningUføreRepositoryImpl(private val connection: DBConnection) : SamordningUføreRepository {

    companion object : Factory<SamordningUføreRepositoryImpl> {
        override fun konstruer(connection: DBConnection): SamordningUføreRepositoryImpl {
            return SamordningUføreRepositoryImpl(connection)
        }
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): VurderingerForSamordningUføre? {
        val query = """
            SELECT * FROM SAMORDNING_UFORE_VURDERING WHERE behandling_id = ? and aktiv = true
        """.trimIndent()
        return connection.queryFirstOrNull(query) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper {
                VurderingerForSamordningUføre(
                    begrunnelse = it.getString("begrunnelse"),
                    vurderingPerioder = hentSamordningUføreVurderingPerioder(it.getLong("id"))
                )
            }
        }
    }

    fun hentSamordningUføreVurderingPerioder(vurderingId: Long): List<SamordningUføreVurderingPeriode> {
        val query = """
            SELECT * FROM SAMORDING_UFORE_VURDERING_PERIODE WHERE vurdering_id = ?
        """.trimIndent()
        return connection.queryList(query) {
            setParams {
                setLong(1, vurderingId)
            }
            setRowMapper {
                SamordningUføreVurderingPeriode(
                    periode = it.getPeriode("periode"),
                    uføregradTilSamordning = Prosent(it.getInt("uforegrad")),
                )
            }
        }
    }

    override fun lagre(
        behandlingId: BehandlingId,
        vurdering: VurderingerForSamordningUføre
    ) {
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)
        if (eksisterendeGrunnlag != null) {
            deaktiverGrunnlag(behandlingId)
        }

        val samordingUføreVurderingQuery = """
            INSERT INTO SAMORDNING_UFORE_VURDERING (behandling_id, begrunnelse) VALUES (?, ?)
        """.trimIndent()

        val vurderingId = connection.executeReturnKey(samordingUføreVurderingQuery) {
            setParams {
                setLong(1, behandlingId.id)
                setString(2, vurdering.begrunnelse)
            }
        }

        val periodeQuery = """
                INSERT INTO SAMORDING_UFORE_VURDERING_PERIODE (periode, vurdering_id, uforegrad) VALUES (?::daterange, ?, ?)
                """.trimIndent()

        connection.executeBatch(periodeQuery, vurdering.vurderingPerioder) {
            setParams {
                setPeriode(1, it.periode)
                setLong(2, vurderingId)
                setInt(3, it.uføregradTilSamordning.prosentverdi())
            }
        }

    }

    private fun deaktiverGrunnlag(behandlingId: BehandlingId) {
        connection.execute("UPDATE SAMORDNING_UFORE_VURDERING set aktiv = false WHERE behandling_id = ? and aktiv = true") {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setResultValidator { require(it == 1) }
        }
    }

    override fun kopier(
        fraBehandling: BehandlingId,
        tilBehandling: BehandlingId
    ) {
        TODO()
    }
}
