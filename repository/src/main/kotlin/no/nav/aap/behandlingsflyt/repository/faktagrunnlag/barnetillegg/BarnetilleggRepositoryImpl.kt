package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.barnetillegg

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.BarnetilleggGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.BarnetilleggPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.BarnetilleggRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.lookup.repository.Factory

class BarnetilleggRepositoryImpl(private val connection: DBConnection) : BarnetilleggRepository {

    companion object : Factory<BarnetilleggRepositoryImpl> {
        override fun konstruer(connection: DBConnection): BarnetilleggRepositoryImpl {
            return BarnetilleggRepositoryImpl(connection)
        }
    }

    override fun hentHvisEksisterer(behandlingsId: BehandlingId): BarnetilleggGrunnlag? {
        val query = """
            SELECT * FROM BARNETILLEGG_GRUNNLAG WHERE behandling_id = ? and aktiv = true
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

    override fun lagre(behandlingId: BehandlingId, barnetilleggPerioder: List<BarnetilleggPeriode>) {
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)
        val eksisterendePerioder = eksisterendeGrunnlag?.perioder ?: emptySet()

        if (eksisterendePerioder != barnetilleggPerioder) {
            if (eksisterendeGrunnlag != null) {
                deaktiverGrunnlag(behandlingId)
            }

            lagreNyttGrunnlag(behandlingId, barnetilleggPerioder)
        }
    }

    override fun slett(behandlingId: BehandlingId) {

        val barnetilleggPerioderIds = getBarnetilleggPerioderIds(behandlingId)
        val barnetilleggPeriodeIds = getBarnetilleggPeriodeIds(barnetilleggPerioderIds)

        connection.execute("""
            delete from barnetillegg_grunnlag where behandling_id = ?; 
            delete from barnetillegg_periode where perioder_id = ANY(?::bigint[]);
            delete from barnetillegg_perioder where id = ANY(?::bigint[]);
            delete from barn_tillegg where barnetillegg_periode_id = ANY(?::bigint[]);
           
        """.trimIndent()) {
            setParams {
                setLong(1, behandlingId.id)
                setLongArray(2, barnetilleggPerioderIds)
                setLongArray(3, barnetilleggPerioderIds)
                setLongArray(4, barnetilleggPeriodeIds)

            }
        }
    }

    private fun getBarnetilleggPerioderIds(behandlingId: BehandlingId): List<Long> = connection.queryList(
        """
                    SELECT perioder_id
                    FROM barnetillegg_grunnlag
                    WHERE behandling_id = ?
                 
                """.trimIndent()
    ) {
        setParams { setLong(1, behandlingId.id) }
        setRowMapper { row ->
            row.getLong("perioder_id")
        }
    }

    private fun getBarnetilleggPeriodeIds(perioderIds: List<Long>): List<Long> = connection.queryList(
        """
                    SELECT id
                    FROM barnetillegg_periode
                    WHERE perioder_id = ANY(?::bigint[]);
                 
                """.trimIndent()
    ) {
        setParams { setLongArray(1, perioderIds) }
        setRowMapper { row ->
            row.getLong("id")
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
        val insertBarnQuery = """
            INSERT INTO BARN_TILLEGG (ident, barnetillegg_periode_id) VALUES (?, ?)
        """.trimIndent()

        barnetilleggPerioder.forEach{ periode ->
            val periodeId = connection.executeReturnKey(query){
                setParams {
                    setLong(1, perioderId)
                    setPeriode(2, periode.periode)
                }
            }

            connection.executeBatch(insertBarnQuery,periode.personIdenter){
                setParams { ident ->
                    setString(1,ident.identifikator)
                    setLong(2,periodeId)
                }
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

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        val eksisterendeGrunnlag = hentHvisEksisterer(fraBehandling)
        if (eksisterendeGrunnlag == null) {
            return
        }
        val query = """
            INSERT INTO BARNETILLEGG_GRUNNLAG (behandling_id, perioder_id) SELECT ?, perioder_id from BARNETILLEGG_GRUNNLAG where behandling_id = ? and aktiv
        """.trimIndent()

        connection.execute(query) {
            setParams {
                setLong(1, tilBehandling.toLong())
                setLong(2, fraBehandling.toLong())
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
        val periodeneId = row.getLong("perioder_id")

        val query = """
            SELECT * FROM BARNETILLEGG_PERIODE WHERE perioder_id = ?
        """.trimIndent()

        val barnetilleggPerioder = connection.queryList(query) {
            setParams {
                setLong(1, periodeneId)
            }
            setRowMapper {
                mapPeriode(it)
            }
        }.toList()

        return BarnetilleggGrunnlag(row.getLong("id"), barnetilleggPerioder)
    }

    private fun mapPeriode(periodeRow: Row): BarnetilleggPeriode {

        val query = """
            SELECT IDENT FROM BARN_TILLEGG WHERE BARNETILLEGG_PERIODE_ID = ?
        """.trimIndent()

        val identer = connection.querySet(query = query) {
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