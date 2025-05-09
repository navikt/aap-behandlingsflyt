package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.samordning

import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.SamordningGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.SamordningPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.SamordningRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.lookup.repository.Factory

class SamordningRepositoryImpl(private val connection: DBConnection) : SamordningRepository {

    companion object : Factory<SamordningRepositoryImpl> {
        override fun konstruer(connection: DBConnection): SamordningRepositoryImpl {
            return SamordningRepositoryImpl(connection)
        }
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): SamordningGrunnlag? {
        val query = """
            SELECT * FROM SAMORDNING_GRUNNLAG WHERE behandling_id = ? and aktiv = true
        """.trimIndent()
        return connection.queryFirstOrNull(query) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper {
                mapGrunnlag(it)
            }
        }
    }

    private fun mapGrunnlag(row: Row): SamordningGrunnlag {
        val perioderId = row.getLong("perioder_id")

        val query = """
            SELECT * FROM SAMORDNING_PERIODE WHERE perioder_id = ?
        """.trimIndent()

        val samordningPerioder = connection.queryList(query) {
            setParams {
                setLong(1, perioderId)
            }
            setRowMapper {
                SamordningPeriode(
                    it.getPeriode("periode"),
                    Prosent(it.getInt("gradering"))
                )
            }
        }.toList()

        return SamordningGrunnlag(row.getLong("id"), samordningPerioder)
    }

    override fun lagre(behandlingId: BehandlingId, samordningPerioder: List<SamordningPeriode>, input: Faktagrunnlag) {
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)
        val eksisterendePerioder = eksisterendeGrunnlag?.samordningPerioder ?: emptySet()

        if (eksisterendePerioder != samordningPerioder) {
            if (eksisterendeGrunnlag != null) {
                deaktiverGrunnlag(behandlingId)
            }

            lagreNyttGrunnlag(behandlingId, samordningPerioder, input)
        }
    }

    private fun lagreNyttGrunnlag(
        behandlingId: BehandlingId,
        samordningPerioder: List<SamordningPeriode>,
        input: Faktagrunnlag
    ) {
        val samordningeneQuery = """
            INSERT INTO SAMORDNING_PERIODER DEFAULT VALUES
            """.trimIndent()
        val perioderId = connection.executeReturnKey(samordningeneQuery)

        val query = """
            INSERT INTO SAMORDNING_PERIODE (perioder_id, periode, gradering) VALUES (?, ?::daterange, ?)
            """.trimIndent()
        connection.executeBatch(query, samordningPerioder) {
            setParams {
                setLong(1, perioderId)
                setPeriode(2, it.periode)
                setInt(3, it.gradering.prosentverdi())
            }
        }

        val grunnlagQuery = """
            INSERT INTO SAMORDNING_GRUNNLAG (behandling_id, perioder_id) VALUES (?, ?)
        """.trimIndent()
        connection.execute(grunnlagQuery) {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, perioderId)
            }
        }

        // TODO: lagre faktagrunnlag for sporing
    }

    private fun deaktiverGrunnlag(behandlingId: BehandlingId) {
        connection.execute("UPDATE SAMORDNING_GRUNNLAG set aktiv = false WHERE behandling_id = ? and aktiv = true") {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setResultValidator { require(it == 1) }
        }
    }

    override fun slett(behandlingId: BehandlingId) {

        val smaordningPerioderIds = getSamordningPerioderIds(behandlingId)

        connection.execute("""
            delete from samordning_grunnlag where behandling_id = ?; 
            delete from samordning_periode where perioder_id = ANY(?::bigint[]);
            delete from samordning_perioder where id = ANY(?::bigint[]);
          
        """.trimIndent()) {
            setParams {
                setLong(1, behandlingId.id)
                setLongArray(2, smaordningPerioderIds)
                setLongArray(3, smaordningPerioderIds)
            }
        }
    }

    private fun getSamordningPerioderIds(behandlingId: BehandlingId): List<Long> = connection.queryList(
        """
                    SELECT perioder_id
                    FROM samordning_grunnlag
                    WHERE behandling_id = ?
                 
                """.trimIndent()
    ) {
        setParams { setLong(1, behandlingId.id) }
        setRowMapper { row ->
            row.getLong("perioder_id")
        }
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        val eksisterendeGrunnlag = hentHvisEksisterer(fraBehandling)
        if (eksisterendeGrunnlag == null) {
            return
        }
        val query = """
            INSERT INTO SAMORDNING_GRUNNLAG (behandling_id, perioder_id) SELECT ?, perioder_id from SAMORDNING_GRUNNLAG where behandling_id = ? and aktiv
        """.trimIndent()

        connection.execute(query) {
            setParams {
                setLong(1, tilBehandling.toLong())
                setLong(2, fraBehandling.toLong())
            }
        }
    }
}