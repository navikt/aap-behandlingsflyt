package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.samordning

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreVurderingPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.lookup.repository.Factory
import org.slf4j.LoggerFactory

class SamordningUføreRepositoryImpl(private val connection: DBConnection) : SamordningUføreRepository {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object : Factory<SamordningUføreRepositoryImpl> {
        override fun konstruer(connection: DBConnection): SamordningUføreRepositoryImpl {
            return SamordningUføreRepositoryImpl(connection)
        }
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): SamordningUføreGrunnlag? {
        val query = """
            SELECT * FROM SAMORDNING_UFORE_GRUNNLAG WHERE behandling_id = ? and aktiv = true
        """.trimIndent()
        return connection.queryFirstOrNull(query) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper {
                SamordningUføreGrunnlag(
                    vurdering = SamordningUføreVurdering(
                        begrunnelse = hentSamordningUføreVurderingBegrunnelse(it.getLong("vurdering_id")),
                        vurderingPerioder = hentSamordningUføreVurderingPerioder(it.getLong("vurdering_id"))
                    )
                )
            }
        }
    }

    fun hentSamordningUføreVurderingBegrunnelse(vurderingId: Long): String {
        val query = """
            SELECT * FROM SAMORDNING_UFORE_VURDERING WHERE id = ?
        """.trimIndent()
        return connection.queryFirst(query) {
            setParams {
                setLong(1, vurderingId)
            }
            setRowMapper {
                it.getString("begrunnelse")

            }
        }
    }

    fun hentSamordningUføreVurderingPerioder(vurderingId: Long): List<SamordningUføreVurderingPeriode> {
        val query = """
            SELECT * FROM SAMORDNING_UFORE_VURDERING_PERIODE WHERE vurdering_id = ?
        """.trimIndent()
        return connection.queryList(query) {
            setParams {
                setLong(1, vurderingId)
            }
            setRowMapper {
                SamordningUføreVurderingPeriode(
                    virkningstidspunkt = it.getLocalDate("virkningstidspunkt"),
                    uføregradTilSamordning = Prosent(it.getInt("uforegrad")),
                )
            }
        }
    }

    override fun lagre(
        behandlingId: BehandlingId,
        vurdering: SamordningUføreVurdering
    ) {
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)
        if (eksisterendeGrunnlag != null) {
            deaktiverGrunnlag(behandlingId)
        }

        val samordingUføreVurderingQuery = """
            INSERT INTO SAMORDNING_UFORE_VURDERING (begrunnelse) VALUES (?)
        """.trimIndent()

        val vurderingId = connection.executeReturnKey(samordingUføreVurderingQuery) {
            setParams {
                setString(1, vurdering.begrunnelse)
            }
        }

        val samordingUføreGrunnlagQuery = """
            INSERT INTO SAMORDNING_UFORE_GRUNNLAG (behandling_id, vurdering_id) VALUES (?, ?)
        """.trimIndent()

        connection.execute(samordingUføreGrunnlagQuery) {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, vurderingId)
            }
        }

        val periodeQuery = """
                INSERT INTO SAMORDNING_UFORE_VURDERING_PERIODE (virkningstidspunkt, vurdering_id, uforegrad) VALUES (?, ?, ?)
                """.trimIndent()

        connection.executeBatch(periodeQuery, vurdering.vurderingPerioder) {
            setParams {
                setLocalDate(1, it.virkningstidspunkt)
                setLong(2, vurderingId)
                setInt(3, it.uføregradTilSamordning.prosentverdi())
            }
        }

    }

    override fun slett(behandlingId: BehandlingId) {

        val samordningUforeVurderingIds = getSamordningUforeVurderingIds(behandlingId)

        val deletedRows = connection.executeReturnUpdated("""
            delete from samordning_ufore_grunnlag where behandling_id = ?; 
            delete from samordning_ufore_vurdering_periode where vurdering_id = ANY(?::bigint[]);
            delete from samordning_ufore_vurdering where id = ANY(?::bigint[]);
          
        """.trimIndent()) {
            setParams {
                setLong(1, behandlingId.id)
                setLongArray(2, samordningUforeVurderingIds)
                setLongArray(3, samordningUforeVurderingIds)
            }
        }
        log.info("Slettet $deletedRows fra samordning_ufore_grunnlag")
    }

    private fun getSamordningUforeVurderingIds(behandlingId: BehandlingId): List<Long> = connection.queryList(
        """
                    SELECT vurdering_id
                    FROM samordning_ufore_grunnlag
                    WHERE behandling_id = ? AND vurdering_id is not null
                 
                """.trimIndent()
    ) {
        setParams { setLong(1, behandlingId.id) }
        setRowMapper { row ->
            row.getLong("vurdering_id")
        }
    }

    private fun deaktiverGrunnlag(behandlingId: BehandlingId) {
        connection.execute("UPDATE SAMORDNING_UFORE_GRUNNLAG set aktiv = false WHERE behandling_id = ? and aktiv = true") {
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
        require(fraBehandling != tilBehandling)
        connection.execute("INSERT INTO SAMORDNING_UFORE_GRUNNLAG (BEHANDLING_ID, VURDERING_ID) SELECT ?, VURDERING_ID FROM SAMORDNING_UFORE_GRUNNLAG WHERE AKTIV AND BEHANDLING_ID = ?") {
            setParams {
                setLong(1, tilBehandling.toLong())
                setLong(2, fraBehandling.toLong())
            }
        }
    }
}
