package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.dbconnect.Row
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import no.nav.aap.verdityper.sakogbehandling.Ident

class BarnVurderingRepository(private val connection: DBConnection) {

    fun hentHvisEksisterer(behandlingsId: BehandlingId): BarnVurderingGrunnlag? {
        val query = """
            SELECT * FROM BARN_VURDERING_GRUNNLAG WHERE behandling_id = ? and aktiv = true
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

    fun lagre(behandlingId: BehandlingId, barnVurderingPeriode: Set<BarnVurderingPeriode>) {
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)
        val eksisterendePerioder = eksisterendeGrunnlag?.vurdering?.barn ?: emptySet()

        if (eksisterendePerioder != barnVurderingPeriode) {
            if (eksisterendeGrunnlag != null) {
                deaktiverGrunnlag(behandlingId)
            }

            lagreNyttGrunnlag(behandlingId, barnVurderingPeriode)
        }
    }

    private fun lagreNyttGrunnlag(behandlingId: BehandlingId, barneVurderingPerioder: Set<BarnVurderingPeriode>) {
        val barnetilleggPeriodeQuery = """
            INSERT INTO BARN_VURDERING_PERIODER DEFAULT VALUES
            """.trimIndent()
        val perioderId = connection.executeReturnKey(barnetilleggPeriodeQuery)


        val query = """
            INSERT INTO BARN_VURDERING_PERIODE (perioder_id, periode) VALUES (?, ?::daterange)
            """.trimIndent()
        val insertBarnQuery = """
            INSERT INTO BARN_VURDERING (ident, barn_vurdering_periode_id) VALUES (?, ?)
        """.trimIndent()

        barneVurderingPerioder.forEach{ periode ->
            val periodeId = connection.executeReturnKey(query){
                setParams {
                    setLong(1, perioderId)
                    setPeriode(2, periode.periode)
                }
            }

            connection.executeBatch(insertBarnQuery,periode.barn){
                setParams { ident ->
                    setString(1,ident.identifikator)
                    setLong(2,periodeId)
                }
            }
        }

        val grunnlagQuery = """
            INSERT INTO BARN_VURDERING_GRUNNLAG (behandling_id, perioder_id) VALUES (?, ?)
        """.trimIndent()
        connection.execute(grunnlagQuery) {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, perioderId)
            }
        }
    }

    fun kopier(fraBehandlingId: BehandlingId, tilBehandlingId: BehandlingId) {
        val eksisterendeGrunnlag = hentHvisEksisterer(fraBehandlingId)
        if (eksisterendeGrunnlag == null) {
            return
        }
        val query = """
            INSERT INTO BARN_VURDERING_GRUNNLAG (behandling_id, perioder_id) SELECT ?, perioder_id from BARN_VURDERING_GRUNNLAG where behandling_id = ? and aktiv
        """.trimIndent()

        connection.execute(query) {
            setParams {
                setLong(1, tilBehandlingId.toLong())
                setLong(2, fraBehandlingId.toLong())
            }
        }
    }

    private fun deaktiverGrunnlag(behandlingId: BehandlingId) {
        connection.execute("UPDATE BARN_VURDERING_GRUNNLAG set aktiv = false WHERE behandling_id = ? and aktiv = true") {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setResultValidator { require(it == 1) }
        }
    }

    private fun mapGrunnlag(row: Row): BarnVurderingGrunnlag {
        val periodeneId = row.getLong("perioder_id")

        val query = """
            SELECT * FROM BARN_VURDERING_PERIODE WHERE perioder_id = ?
        """.trimIndent()

        val barneVurderingPerioder = connection.queryList(query) {
            setParams {
                setLong(1, periodeneId)
            }
            setRowMapper {
                mapPeriode(it)
            }
        }.toSet()

        return BarnVurderingGrunnlag(row.getLong("id"), BehandlingId(row.getLong("BEHANDLING_ID")),BarnVurdering(barneVurderingPerioder))
    }

    private fun mapPeriode(periodeRow: Row): BarnVurderingPeriode {

        val query = """
            SELECT IDENT FROM BARN_VURDERING WHERE BARN_VURDERING_PERIODE_ID = ?
        """.trimIndent()

        val identer = connection.querySet(query = query) {
            setParams {
                setLong(1, periodeRow.getLong("ID"))
            }
            setRowMapper { Ident(it.getString("IDENT"))  }
        }

        return BarnVurderingPeriode(
            identer,
            periodeRow.getPeriode("periode")
        )
    }
}