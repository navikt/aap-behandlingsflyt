package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.samordning.SamordningGraderingVurderingDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.samordning.SamordningGrunnlagDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.samordning.YtelsesVurderingDto
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

class SamordningRepository (private val connection: DBConnection){

    fun hent(behandlingId: BehandlingId): SamordningGrunnlagDto {
        return requireNotNull(hentHvisEksisterer(behandlingId))
    }

    fun hentHvisEksisterer(behandlingId: BehandlingId): SamordningGrunnlagDto? {
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

    //TODO: FIKS mapGrunnlag og mapPeriode, gir ikke helt mening som er nå, må lande på kontrakt
    private fun mapGrunnlag(row: Row): SamordningGrunnlagDto {
        val samordningeneId = row.getLong("perioder_id")

        val query = """
            SELECT * FROM SAMORDNING_PERIODE WHERE perioder_id = ?
        """.trimIndent()

        val samordningPerioder = connection.queryList(query) {
            setParams {
                setLong(1, samordningeneId)
            }
            setRowMapper {
                mapPeriode(it)
            }
        }.toList()

        return SamordningGrunnlagDto(
            SamordningGraderingVurderingDto(
                "BEGRUNNELSE HER",
                samordningPerioder
            )
        )
    }

    private fun mapPeriode(it: Row): YtelsesVurderingDto {
        return YtelsesVurderingDto(
            it.getString("ytelse"),
            it.getInt("gradering")
        )
    }


    fun lagre(behandlingId: BehandlingId, samordningPerioder: List<SamordningPeriode>) {
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)
        val eksisterendePerioder = eksisterendeGrunnlag?.samordningGraderingVurdering?.ytelsesVurderinger ?: emptySet()

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
            setParams { periode ->
                setLong(1, perioderId)
                setPeriode(2, periode.periode)
                setInt(3, periode.gradering.prosentverdi())
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