package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.foreldrepenger

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.verdityper.Prosent
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

class ForeldrepengerRepository (private val connection: DBConnection){
    fun hentHvisEksisterer(behandlingId: BehandlingId): ForeldrepengerGrunnlag? {
        val query = """
            SELECT * FROM FORELDREPENGER_GRUNNLAG WHERE behandling_id = ? and aktiv = true
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

    private fun mapGrunnlag(row: Row): ForeldrepengerGrunnlag {
        val foreldrepengerPerioderId = row.getLong("perioder_id")

        val query = """
            SELECT * FROM FORELDREPENGER_PERIODE WHERE perioder_id = ?
        """.trimIndent()

        val foreldrepengerPerioder = connection.queryList(query) {
            setParams {
                setLong(1, foreldrepengerPerioderId)
            }
            setRowMapper {
                mapPeriode(it)
            }
        }.toList()

        return ForeldrepengerGrunnlag(foreldrepengerPerioderId, foreldrepengerPerioder)
    }

    private fun mapPeriode(it: Row): ForeldrepengerPeriode {
        return ForeldrepengerPeriode(
            it.getPeriode("periode"),
            Prosent(it.getInt("gradering")),
            it.getString("ytelse")
        )
    }

    fun lagre(behandlingId: BehandlingId, foreldrepengerPerioder: List<ForeldrepengerPeriode>) {
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)
        val eksisterendePerioder = eksisterendeGrunnlag?.perioder ?: emptySet()

        if (eksisterendePerioder != foreldrepengerPerioder) {
            if (eksisterendeGrunnlag != null) {
                deaktiverGrunnlag(behandlingId)
            }

            lagreNyttGrunnlag(behandlingId, foreldrepengerPerioder)
        }
    }

    private fun lagreNyttGrunnlag(behandlingId: BehandlingId, foreldrepengerPerioder: List<ForeldrepengerPeriode>) {
        val forereldrepengerQuery = """
            INSERT INTO FORELDREPENGER_PERIODER DEFAULT VALUES
            """.trimIndent()
        val perioderId = connection.executeReturnKey(forereldrepengerQuery)

        val query = """
            INSERT INTO FORELDREPENGER_PERIODE (perioder_id, periode, gradering, ytelse) VALUES (?, ?::daterange, ?, ?)
            """.trimIndent()
        connection.executeBatch(query, foreldrepengerPerioder) {
            setParams { periode ->
                setLong(1, perioderId)
                setPeriode(2, periode.periode)
                setInt(3, periode.gradering.prosentverdi())
                setString(4, periode.ytelse)
            }
        }

        val grunnlagQuery = """
            INSERT INTO FORELDREPENGER_GRUNNLAG (behandling_id, perioder_id) VALUES (?, ?)
        """.trimIndent()
        connection.execute(grunnlagQuery) {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, perioderId)
            }
        }
    }

    private fun deaktiverGrunnlag(behandlingId: BehandlingId) {
        connection.execute("UPDATE FORELDREPENGER_GRUNNLAG set aktiv = false WHERE behandling_id = ? and aktiv = true") {
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
            INSERT INTO FORELDREPENGER_GRUNNLAG (behandling_id, perioder_id) SELECT ?, perioder_id from FORELDREPENGER_GRUNNLAG where behandling_id = ? and aktiv
        """.trimIndent()

        connection.execute(query) {
            setParams {
                setLong(1, tilBehandlingId.toLong())
                setLong(2, fraBehandlingId.toLong())
            }
        }
    }
}