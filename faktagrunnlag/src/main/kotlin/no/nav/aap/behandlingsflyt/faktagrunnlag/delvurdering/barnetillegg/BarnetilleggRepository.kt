package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.dbconnect.Row
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import no.nav.aap.verdityper.sakogbehandling.Ident

class BarnetilleggRepository(private val connection: DBConnection) {

    fun hentHvisEksisterer(behandlingsId: BehandlingId): BarnetilleggGrunnlag? {
        val query = """
            SELECT * FROM BARNETILLEGG_PERIODER WHERE behandling_id = ? and aktiv = true
        """.trimIndent()
        return connection.queryFirstOrNull(query) {
            setParams {
                setLong(1, behandlingsId.toLong())
            }
            setRowMapper {
                mapGrunnlag(it)
            }
        }

    }

    fun lagre(behandlingId: BehandlingId, barnetilleggPerioder: List<BarnetilleggPeriode>) {
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)
        val eksisterendePerioder = eksisterendeGrunnlag?.perioder ?: emptySet()

        if (eksisterendePerioder != barnetilleggPerioder) {
            if (eksisterendeGrunnlag != null) {
                deaktiverGrunnlag(behandlingId)
            }

            lagreNyttGrunnlag(behandlingId, barnetilleggPerioder)
        }
    }

    private fun lagreNyttGrunnlag(behandlingId: BehandlingId, barnetilleggPerioder: List<BarnetilleggPeriode>) {
        val barnetilleggPeriodeQuery = """
            INSERT INTO BARNETILLEGG_PERIODER DEFAULT VALUES
            """.trimIndent()
        val perioderId = connection.executeReturnKey(barnetilleggPeriodeQuery)

        val query = """
            INSERT INTO BARNETILLEGG_PERIODE (perioder_id, periode) VALUES (?, ?::daterange)
            """.trimIndent()
        connection.executeBatch(query, barnetilleggPerioder) {
            setParams { periode ->
                setLong(1, perioderId)
                setPeriode(2, periode.periode)
            }
        }

        val grunnlagQuery = """
            INSERT INTO BARNETILLEGG_GRUNNLAG (behandling_id, perioder_id) VALUES (?, ?)
        """.trimIndent()
        connection.execute(grunnlagQuery) {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, perioderId)
            }
        }
    }

    private fun deaktiverGrunnlag(behandlingId: BehandlingId) {
        connection.execute("UPDATE BARNETILLEGG_GRUNNLAG set aktiv = false WHERE behandling_id = ? and aktiv = true") {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setResultValidator { require(it == 1) }
        }
    }

    private fun mapGrunnlag(row: Row): BarnetilleggGrunnlag {
        val pliktkorteneId = row.getLong("perioder_id")

        val query = """
            SELECT * FROM BARNETILLEGG_PERIODE WHERE perioder_id = ?
        """.trimIndent()

        val barnetilleggPerioder = connection.queryList(query) {
            setParams {
                setLong(1, pliktkorteneId)
            }
            setRowMapper {
                mapPeriode(it)
            }
        }.toList()

        return BarnetilleggGrunnlag(row.getLong("id"), barnetilleggPerioder)
    }

    private fun mapPeriode(periodeRow: Row): BarnetilleggPeriode {

        val query = """
            SELECT IDENT FROM BARN WHERE BARNETILLEGG_PERIODE_ID = ?
        """.trimIndent()

        val identer = connection.queryList(query = query) {
            setParams {
                setLong(1, periodeRow.getLong("ID"))
            }
            setRowMapper { Ident(it.getString("IDENT"))  }
        }

        return BarnetilleggPeriode(
            periodeRow.getPeriode("periode"),
            identer
        )
    }
}