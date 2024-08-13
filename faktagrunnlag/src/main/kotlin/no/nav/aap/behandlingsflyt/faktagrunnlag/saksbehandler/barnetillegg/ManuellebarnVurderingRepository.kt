package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barnetillegg

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.dbconnect.Row
import no.nav.aap.verdityper.Periode
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import no.nav.aap.verdityper.sakogbehandling.Ident

class ManuellebarnVurderingRepository(private val connection: DBConnection) {

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

    fun lagre(behandlingId: BehandlingId, manueltBarnVurdeirng: Set<ManueltBarnVurdeirng>) {
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)
        val eksisterendePerioder = eksisterendeGrunnlag?.vurdering?.barn ?: emptySet()

        if (eksisterendePerioder != manueltBarnVurdeirng) {
            if (eksisterendeGrunnlag != null) {
                deaktiverGrunnlag(behandlingId)
            }

            lagreNyttGrunnlag(behandlingId, manueltBarnVurdeirng)
        }
    }

    private fun lagreNyttGrunnlag(behandlingId: BehandlingId, barneVurderingPerioder: Set<ManueltBarnVurdeirng>) {
        val barnetilleggPeriodeQuery = """
            INSERT INTO BARN_VURDERING_PERIODER DEFAULT VALUES
            """.trimIndent()
        val perioderId = connection.executeReturnKey(barnetilleggPeriodeQuery)

        lagreBarnVurdering(perioderId, barneVurderingPerioder)

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

    private fun lagreBarnVurdering(perioderId: Long, manueltBarnVurderingPerioder: Set<ManueltBarnVurdeirng>) {
        val query = """
            INSERT INTO BARN_VURDERING (perioder_id, ident, BEGRUNNELSE, SKAL_BEREGNES_BARNETILLEGG) VALUES (?, ?, ?, ?)
            """.trimIndent()

        manueltBarnVurderingPerioder.forEach { barn ->
            val barnVurderingId = connection.executeReturnKey(query) {
                setParams {
                    setLong(1, perioderId)
                    setString(2, barn.ident.identifikator)
                    setString(3, barn.begrunnelse)
                    setBoolean(4, barn.skalBeregnesBarnetillegg)
                }
            }

            connection.executeBatch("""INSERT INTO BARN_VURDERING_PERIODE (VURDERING_ID, PERIODE) VALUES (?, ?::daterange)""", barn.perioder) {
                setParams { periode ->
                    setLong(1, barnVurderingId)
                    setPeriode(2, periode)
                }
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
            SELECT * FROM BARN_VURDERING WHERE perioder_id = ?
        """.trimIndent()

        val barneVurderingPerioder = connection.queryList(query) {
            setParams {
                setLong(1, periodeneId)
            }
            setRowMapper {
                mapPeriode(it)
            }
        }.toSet()

        return BarnVurderingGrunnlag(
            row.getLong("id"),
            BehandlingId(row.getLong("BEHANDLING_ID")),
            ManuelleBarnVurdeirng(barneVurderingPerioder)
        )
    }

    private fun mapPeriode(row: Row): ManueltBarnVurdeirng {

        return ManueltBarnVurdeirng(
            ident = Ident(row.getString("IDENT")),
            begrunnelse = row.getString("BEGRUNNELSE"),
            skalBeregnesBarnetillegg = row.getBoolean("SKAL_BEREGNES_BARNETILLEGG"),
            perioder = getPerioderForBarn(row.getLong("ID"))
        )
    }

    private fun getPerioderForBarn(barnVurdeirngId: Long): List<Periode> {
        return connection.queryList("""SELECT * FROM BARN_VURDERING_PERIODE WHERE VURDERING_ID = ?""") {
            setParams {
                setLong(1, barnVurdeirngId)
            }
            setRowMapper { row ->
                row.getPeriode("PERIODE")
            }
        }
    }


}