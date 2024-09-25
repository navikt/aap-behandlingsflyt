package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.verdityper.Prosent
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

class SamordningRepository (private val connection: DBConnection){

    fun hentHvisEksisterer(behandlingId: BehandlingId): SamordningGrunnlag? {
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

    fun lagre(behandlingId: BehandlingId, samordningPerioder: List<SamordningPeriode>) {
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)
        val eksisterendePerioder = eksisterendeGrunnlag?.samordningPerioder ?: emptySet()

        if (eksisterendePerioder != samordningPerioder) {
            if (eksisterendeGrunnlag != null) {
                deaktiverGrunnlag(behandlingId)
            }

            lagreNyttGrunnlag(behandlingId, samordningPerioder)
        }
    }

    private fun lagreNyttGrunnlag(behandlingId: BehandlingId, samordningPerioder: List<SamordningPeriode>) {
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
    }

    private fun deaktiverGrunnlag(behandlingId: BehandlingId) {
        connection.execute("UPDATE SAMORDNING_GRUNNLAG set aktiv = false WHERE behandling_id = ? and aktiv = true") {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setResultValidator { require(it == 1) }
        }
    }

    fun kopier(fraBehandlingId: BehandlingId, tilBehandlingId: BehandlingId) {
        val eksisterendeGrunnlag = hentHvisEksisterer(fraBehandlingId)
        if (eksisterendeGrunnlag == null) {
            return
        }
        val query = """
            INSERT INTO SAMORDNING_GRUNNLAG (behandling_id, perioder_id) SELECT ?, perioder_id from SAMORDNING_GRUNNLAG where behandling_id = ? and aktiv
        """.trimIndent()

        connection.execute(query) {
            setParams {
                setLong(1, tilBehandlingId.toLong())
                setLong(2, fraBehandlingId.toLong())
            }
        }
    }
}