package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.sykepenger

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.verdityper.Prosent
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

class SykepengerRepository (private val connection: DBConnection){
    fun hentHvisEksisterer(behandlingId: BehandlingId): SykepengerGrunnlag? {
        val query = """
            SELECT * FROM SYKEPENGER_GRUNNLAG WHERE behandling_id = ? and aktiv = true
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

    private fun mapGrunnlag(row: Row): SykepengerGrunnlag {
        val sykepengerPerioderId = row.getLong("perioder_id")

        val query = """
            SELECT * FROM SYKEPENGER_PERIODE WHERE perioder_id = ?
        """.trimIndent()

        val sykepengerPerioder = connection.queryList(query) {
            setParams {
                setLong(1, sykepengerPerioderId)
            }
            setRowMapper {
                mapPeriode(it)
            }
        }.toList()

        return SykepengerGrunnlag(sykepengerPerioderId, sykepengerPerioder)
    }

    private fun mapPeriode(it: Row): SykepengerPeriode {
        return SykepengerPeriode(
            it.getPeriode("periode"),
            Prosent(it.getInt("gradering")),
            it.getInt("kronesum"),
            it.getString("ytelse")
        )
    }

    fun lagre(behandlingId: BehandlingId, sykepengerPerioder: List<SykepengerPeriode>) {
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)
        val eksisterendePerioder = eksisterendeGrunnlag?.perioder ?: emptySet()

        if (eksisterendePerioder != sykepengerPerioder) {
            if (eksisterendeGrunnlag != null) {
                deaktiverGrunnlag(behandlingId)
            }

            lagreNyttGrunnlag(behandlingId, sykepengerPerioder)
        }
    }

    private fun lagreNyttGrunnlag(behandlingId: BehandlingId, sykepengerPerioder: List<SykepengerPeriode>) {
        val sykepengerQuery = """
            INSERT INTO SYKEPENGER_PERIODER DEFAULT VALUES
            """.trimIndent()
        val perioderId = connection.executeReturnKey(sykepengerQuery)

        val query = """
            INSERT INTO SYKEPENGER_PERIODE (perioder_id, periode, gradering, kronesum, ytelse) VALUES (?, ?::daterange, ?, ?, ?)
            """.trimIndent()
        connection.executeBatch(query, sykepengerPerioder) {
            setParams { periode ->
                setLong(1, perioderId)
                setPeriode(2, periode.periode)
                setInt(3, periode.gradering.prosentverdi())
                setInt(4, periode.kronesum?.toInt())
                setString(5, periode.ytelse)
            }
        }

        val grunnlagQuery = """
            INSERT INTO SYKEPENGER_GRUNNLAG (behandling_id, perioder_id) VALUES (?, ?)
        """.trimIndent()
        connection.execute(grunnlagQuery) {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, perioderId)
            }
        }
    }

    private fun deaktiverGrunnlag(behandlingId: BehandlingId) {
        connection.execute("UPDATE SYKEPENGER_GRUNNLAG set aktiv = false WHERE behandling_id = ? and aktiv = true") {
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
            INSERT INTO SYKEPENGER_GRUNNLAG (behandling_id, perioder_id) SELECT ?, perioder_id from SYKEPENGER_GRUNNLAG where behandling_id = ? and aktiv
        """.trimIndent()

        connection.execute(query) {
            setParams {
                setLong(1, tilBehandlingId.toLong())
                setLong(2, fraBehandlingId.toLong())
            }
        }
    }
}