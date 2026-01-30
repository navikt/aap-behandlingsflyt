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
        val eksisterendeDagpengerGrunnlag = hent(behandlingId)

        if (eksisterendeDagpengerGrunnlag.isNotEmpty()) {
            deaktiverEkstisterende(behandlingId)
        }

       val dpPeriode = connection.executeReturnKey("""INSERT INTO DAGPENGER_PERIODER DEFAULT VALUES""")

        connection.execute(
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

    override fun slett(behandlingId: BehandlingId) {
        val perioderIds= getPerioderId(behandlingId)


        connection.execute("""
            DELETE FROM DAGPENGER_GRUNNLAG WHERE BEHANDLING_ID = ?;
            DELETE FROM DAGPENGER_PERIODE WHERE DAGPENGER_PERIODER_ID IN ANY(?::BigInt[]);
            DELETE FROM DAGPENGER_PERIODER WHERE ID = ANY(?::BigInt[]);
            """.trimIndent()){
            setParams {
                setLong(1, behandlingId.id)
                setLongArray(2, perioderIds)
                setLongArray(3, perioderIds)
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

    private fun deaktiverEkstisterende(behandlingId: BehandlingId){
        connection.execute(
            """UPDATE DAGPENGER_GRUNNLAG SET AKTIV = FALSE WHERE BEHANDLING_ID = ? AND AKTIV = TRUE"""
        ) {
            setParams {
                setLong(1, behandlingId.id)
            }
            setResultValidator { rowsUpdated ->
                require(rowsUpdated==1)

            }
        }
    }

    private fun getPerioderId(behandlingId: BehandlingId):List<Long>{
        return connection.queryList(
            """SELECT DAGPENGER_PERIODER_ID
            FROM DAGPENGER_GRUNNLAG
            WHERE BEHANDLING_ID = ? AND AKTIV""".trimIndent()
        ){
            setParams {
                setLong(1, behandlingId.id)
            }
            setRowMapper { row ->
                row.getLong("DAGPENGER_PERIODER_ID")
            }
        }
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        require(fraBehandling != tilBehandling) {
            "Kan ikke kopiere dagpengergrunnlag til samme behandling"
        }
        connection.execute("""INSERT INTO DAGPENGER_GRUNNLAG (BEHANDLING_ID, DAGPENGER_PERIODER_ID) 
                SELECT ?, DAGPENGER_PERIODER_ID 
                FROM DAGPENGER_GRUNNLAG 
                WHERE AKTIV AND BEHANDLING_ID = ?
            """.trimIndent()) {
            setParams {
                setLong(1, tilBehandling.id)
                setLong(2, fraBehandling.id)
            }
        }
    }
}
