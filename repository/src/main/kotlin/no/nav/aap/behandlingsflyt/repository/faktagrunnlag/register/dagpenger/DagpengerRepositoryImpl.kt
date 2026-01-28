package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.register.dagpenger

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.gateway.DagpengerKilde
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.gateway.DagpengerPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.gateway.DagpengerYtelseType
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.dagpenger.DagpengerRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.Factory
import org.slf4j.LoggerFactory

class DagpengerRepositoryImpl(private val connection: DBConnection) : DagpengerRepository {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object : Factory<DagpengerRepositoryImpl>{
        override fun konstruer(connection: DBConnection): DagpengerRepositoryImpl {
            return DagpengerRepositoryImpl(connection)
        }
    }

    override fun lagre(
        behandlingId: BehandlingId,
        dagpengerPeriode: List<DagpengerPeriode>
    ) {
       val dpPeriode = connection.executeReturnKey("""INSERT INTO DAGPENGER_PERIODER DEFAULT VALUES""")

        val dagpengerGrunnlag = connection.executeReturnKey(
            """INSERT INTO DAGPENGER_GRUNNLAG (AKTIV, BEHANDLING_ID, DAGPENGER_PERIODER_ID)
            VALUES (TRUE, ?, ?)"""
        ){
            setParams {
                setLong(1, behandlingId.id)
                setLong(2, dpPeriode)
            }
        }

        connection.executeBatch("""INSERT INTO DAGPENGER_PERIODE (DAGPENGER_PERIODER_ID, PERIODE, YTELSE_TYPE, KILDE) 
            VALUES (?, ?::daterange, ?, ?)""".trimIndent(), dagpengerPeriode) {
            setParams {periode ->
                setLong(1, dpPeriode)
                setPeriode(2, periode.periode)
                setString(3, periode.dagpengerYtelseType.name)
                setString(4, periode.kilde.name)
            }
        }

    }

    override fun hent(
        behandlingId: BehandlingId,
    ) : List<DagpengerPeriode>{
        return connection.queryList(
            """SELECT DP.PERIODE, DP.YTELSE_TYPE, DP.KILDE
            FROM DAGPENGER_PERIODE DP
            JOIN DAGPENGER_PERIODER DPER ON DP.DAGPENGER_PERIODER_ID = DPER.ID
            JOIN DAGPENGER_GRUNNLAG DG ON DPER.ID = DG.DAGPENGER_PERIODER_ID
            WHERE DG.BEHANDLING_ID = ? AND DG.AKTIV = TRUE""".trimIndent()
        ){
            setParams {
                setLong(1, behandlingId.id)
            }
            setRowMapper { row ->
                DagpengerPeriode(
                    periode = row.getPeriode("PERIODE"),
                    dagpengerYtelseType = DagpengerYtelseType.valueOf(row.getString("YTELSE_TYPE")),
                    kilde = DagpengerKilde.valueOf(row.getString("KILDE"))
                )
            }
        }

    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        val dagpengerPerioder = hent(fraBehandling)

        connection.queryFirstOrNull("""
            SELECT DG.DAGPENGER_PERIODER_ID
            FROM DAGPENGER_GRUNNLAG DG
            WHERE DG.BEHANDLING_ID = ? AND DG.AKTIV = TRUE
        """.trimIndent()){
            setParams {
                setLong(1, fraBehandling.id)
            }
            setRowMapper { row ->
                row.getLong("DAGPENGER_PERIODER_ID")
            }
        }?.let { dpPerioderId ->
            val nyttDpPerioderId = connection.executeReturnKey("""INSERT INTO DAGPENGER_PERIODER DEFAULT VALUES""")

            connection.executeBatch("""INSERT INTO DAGPENGER_PERIODE (DAGPENGER_PERIODER_ID, PERIODE, YTELSE_TYPE, KILDE) 
                VALUES (?, ?::daterange, ?, ?)""".trimIndent(), dagpengerPerioder) {
                setParams { periode ->
                    setLong(1, nyttDpPerioderId)
                    setPeriode(2, periode.periode)
                    setString(3, periode.dagpengerYtelseType.name)
                    setString(4, periode.kilde.name)
                }
            }

            connection.execute(
                """UPDATE DAGPENGER_GRUNNLAG SET AKTIV = FALSE WHERE BEHANDLING_ID = ? AND AKTIV = TRUE"""
            ) {
                setParams {
                    setLong(1, tilBehandling.id)
                }
            }

            connection.execute(
                """INSERT INTO DAGPENGER_GRUNNLAG (AKTIV, BEHANDLING_ID, DAGPENGER_PERIODER_ID)
                VALUES (TRUE, ?, ?)"""
            ){
                setParams {
                    setLong(1, tilBehandling.id)
                    setLong(2, nyttDpPerioderId)
                }
            }
        }

    }
}
