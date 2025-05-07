package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.samordning

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelseGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelsePeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelseRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Query
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.lookup.repository.Factory
import org.slf4j.LoggerFactory

class SamordningYtelseRepositoryImpl(private val dbConnection: DBConnection) : SamordningYtelseRepository {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object : Factory<SamordningYtelseRepository> {
        override fun konstruer(connection: DBConnection): SamordningYtelseRepository {
            return SamordningYtelseRepositoryImpl(connection)
        }
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): SamordningYtelseGrunnlag? {
        val sql = """
            SELECT syg.samordning_ytelse_id as syg_samordning_ytelse_id,
                   sy.id                    as sy_id
            FROM SAMORDNING_YTELSE_GRUNNLAG syg
                     left join samordning_ytelser sy on syg.samordning_ytelse_id = sy.id
            WHERE BEHANDLING_ID = ? and aktiv = true
        """.trimIndent()

        val par = dbConnection.queryList(sql, mapGrunnlag(behandlingId))

        if (par.isEmpty()) return null

        return SamordningYtelseGrunnlag(
            grunnlagId = par.first().first,
            ytelser = par.first().second,
        )
    }

    override fun hentEldsteGrunnlag(behandlingId: BehandlingId): SamordningYtelseGrunnlag? {
        val sql = """
            SELECT syg.samordning_ytelse_id as syg_samordning_ytelse_id,
                   sy.id                    as sy_id
            FROM SAMORDNING_YTELSE_GRUNNLAG syg
                     left join samordning_ytelser sy on syg.samordning_ytelse_id = sy.id
            WHERE BEHANDLING_ID = ?
            ORDER BY syg.id ASC
            LIMIT 1
        """.trimIndent()

        val par = dbConnection.queryList(sql, mapGrunnlag(behandlingId))

        if (par.isEmpty()) return null

        return SamordningYtelseGrunnlag(
            grunnlagId = par.first().first,
            ytelser = par.first().second,
        )
    }

    private fun mapGrunnlag(behandlingId: BehandlingId): Query<Pair<Long, List<SamordningYtelse>>>.() -> Unit =
        {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper { row ->
                Pair(
                    row.getLong("syg_samordning_ytelse_id"),
                    hentYtelser(row.getLong("sy_id"))
                )
            }
        }

    private fun hentYtelser(ytelserId: Long): List<SamordningYtelse> {
        val sql = """
            select sy.ytelse_type as sy_ytelse_type,
                   sy.id          as sy_id,
                   sy.kilde       as sy_kilde,
                   sy.saks_ref    as sy_saks_ref
            from samordning_ytelser syr
                     join samordning_ytelse sy on syr.id = sy.ytelser_id
            where syr.id = ?
        """.trimIndent()

        return dbConnection.queryList(sql) {
            setParams {
                setLong(1, ytelserId)
            }
            setRowMapper { row ->
                SamordningYtelse(
                    ytelseType = row.getEnum("sy_ytelse_type"),
                    ytelsePerioder = hentYtelsePerioder(row.getLong("sy_id")),
                    kilde = row.getString("sy_kilde"),
                    saksRef = row.getStringOrNull("sy_saks_ref")
                )
            }
        }
    }

    private fun hentYtelsePerioder(ytelseId: Long): List<SamordningYtelsePeriode> {
        val sql = """
            SELECT * from samordning_ytelse_periode where ytelse_id = ? order by periode
        """.trimIndent()
        return dbConnection.queryList(sql) {
            setParams {
                setLong(1, ytelseId)
            }
            setRowMapper {
                SamordningYtelsePeriode(
                    periode = it.getPeriode("periode"),
                    gradering = it.getIntOrNull("gradering")?.let(::Prosent),
                    kronesum = it.getIntOrNull("kronesum"),
                )
            }
        }
    }

    override fun lagre(behandlingId: BehandlingId, samordningYtelser: List<SamordningYtelse>) {
        val eksiterende = hentHvisEksisterer(behandlingId)
        if (eksiterende != null) {
            deaktiverGrunnlag(behandlingId)
        }

        val samordningYtelserQuery = """
            INSERT INTO SAMORDNING_YTELSER DEFAULT VALUES
            """.trimIndent()
        val ytelserId = dbConnection.executeReturnKey(samordningYtelserQuery)

        samordningYtelser.forEach { ytelse ->
            val ytelseQuery = """
                INSERT INTO SAMORDNING_YTELSE (ytelse_type, ytelser_id, kilde, saks_ref) VALUES (?, ?, ?, ?)
            """.trimIndent()

            val ytelseId = dbConnection.executeReturnKey(ytelseQuery) {
                setParams {
                    setEnumName(1, ytelse.ytelseType)
                    setLong(2, ytelserId)
                    setString(3, ytelse.kilde)
                    setString(4, ytelse.saksRef)
                }
            }

            val ytelsePeriodeQuery = """
                INSERT INTO SAMORDNING_YTELSE_PERIODE (ytelse_id, periode, gradering, kronesum) VALUES (?, ?::daterange, ?, ?)
                """.trimIndent()
            dbConnection.executeBatch(ytelsePeriodeQuery, ytelse.ytelsePerioder) {
                setParams {
                    setLong(1, ytelseId)
                    setPeriode(2, it.periode)
                    setInt(3, it.gradering?.prosentverdi())
                    setInt(4, it.kronesum?.toInt())
                }
            }
        }

        val sql = """
            INSERT INTO SAMORDNING_YTELSE_GRUNNLAG (BEHANDLING_ID, samordning_ytelse_id, aktiv) VALUES (?, ?, ?)
        """.trimIndent()

        val grunnlagId = dbConnection.executeReturnKey(sql) {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, ytelserId)
                setBoolean(3, true)
            }
        }
        log.info("Lagret samordningytelsegrunnlag med id $grunnlagId for behandling $behandlingId.")
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        val eksisterendeGrunnlag = hentHvisEksisterer(fraBehandling)
        if (eksisterendeGrunnlag == null) {
            log.warn("Prøver å kopiere fra behandling $fraBehandling til $tilBehandling, men fant ingen grunnlag for behandlingen")
            return
        }
        val query = """
            INSERT INTO samordning_ytelse_grunnlag 
                (behandling_id, samordning_ytelse_id, aktiv) 
            SELECT ?, samordning_ytelse_id, true
                from samordning_ytelse_grunnlag 
                where behandling_id = ? and aktiv
        """.trimIndent()

        dbConnection.execute(query) {
            setParams {
                setLong(1, tilBehandling.toLong())
                setLong(2, fraBehandling.toLong())
            }
            setResultValidator { require(it == 1) }
            log.info("Kopiert fra behandling $fraBehandling til $tilBehandling")
        }
    }

    override fun slett(behandlingId: BehandlingId) {

        val samordningYtelseIds = getSamordningYtelseIds(behandlingId)

        dbConnection.execute("""
            delete from samordning_ytelse where ytelser_id = ANY(?::bigint[]);
            delete from samordning_ytelse_periode where ytelser_id = ANY(?::bigint[]);
            delete from samordning_ytelser where id = ANY(?::bigint[]);
            delete from samordning_ytelse_grunnlag where behandling_id = ? 
        """.trimIndent()) {
            setParams {
                setLongArray(1, samordningYtelseIds)
                setLongArray(2, samordningYtelseIds)
                setLongArray(3, samordningYtelseIds)
                setLong(4, behandlingId.id)
            }
        }
    }

    private fun getSamordningYtelseIds(behandlingId: BehandlingId): List<Long> = dbConnection.queryList(
        """
                    SELECT samordning_ytelse_id
                    FROM samordning_ytelse_grunnlag
                    WHERE behandling_id = ?
                 
                """.trimIndent()
    ) {
        setParams { setLong(1, behandlingId.id) }
        setRowMapper { row ->
            row.getLong("samordning_ytelse_id")
        }
    }

    private fun deaktiverGrunnlag(behandlingId: BehandlingId) {
        dbConnection.execute("UPDATE SAMORDNING_YTELSE_GRUNNLAG set aktiv = false WHERE behandling_id = ? and aktiv = true") {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setResultValidator { require(it == 1) }
        }
    }

}
