package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.delvurdering.samordning

import no.nav.aap.behandlingsflyt.behandling.samordning.Ytelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelseGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelsePeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.SamordningYtelseRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.lookup.repository.Factory
import org.slf4j.LoggerFactory

class SamordningYtelseRepositoryImpl(private val dbConnection: DBConnection) : SamordningYtelseRepository {
    private val logger = LoggerFactory.getLogger(SamordningYtelseRepositoryImpl::class.java)

    companion object : Factory<SamordningYtelseRepository> {
        override fun konstruer(connection: DBConnection): SamordningYtelseRepository {
         return SamordningYtelseRepositoryImpl(connection)
        }
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): SamordningYtelseGrunnlag? {
        val sql = """
            SELECT *
            FROM SAMORDNING_YTELSE_GRUNNLAG syg
                     left join samordning_ytelse sy on syg.samordning_ytelse_id = sy.id
            WHERE BEHANDLING_ID = ?
        """.trimIndent()

        val par = dbConnection.queryList(sql) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper { row ->
                Pair(
                    row.getLong("SAMORDNING_YTELSE_ID"),
                    row.getEnumOrNull<Ytelse, Ytelse>("ytelse_type")?.let {
                        SamordningYtelse(
                            ytelseType = row.getEnum("ytelse_type"),
                            ytelsePerioder = hentYtelsePerioder(row.getLong("ytelser_id")),
                            kilde = row.getString("kilde"),
                            saksRef = row.getString("saks_ref")
                        )
                    }
                )
            }
        }

        if (par.isEmpty()) return null

        return SamordningYtelseGrunnlag(
            grunnlagId = par.first().first,
            ytelser = par.mapNotNull { it.second }
        )
    }

    private fun hentYtelsePerioder(ytelseId: Long): List<SamordningYtelsePeriode> {
        val sql = """
            SELECT * from samordning_ytelse_periode where ytelse_id = ?
        """.trimIndent()
        return dbConnection.queryList(sql) {
            setParams {
                setLong(1, ytelseId)
            }
            setRowMapper {
                SamordningYtelsePeriode(
                    periode = it.getPeriode("periode"),
                    gradering = it.getIntOrNull("gradering")?.let(::Prosent),
                    kronesum = it.getIntOrNull("kronesum")
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
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        val eksisterendeGrunnlag = hentHvisEksisterer(fraBehandling)
        if (eksisterendeGrunnlag == null) {
            logger.warn("Prøver å kopiere fra behandling $fraBehandling til $tilBehandling, men fant ingen grunnlag for behandlingen")
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
            logger.info("Kopiert fra behandling $fraBehandling til $tilBehandling")
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