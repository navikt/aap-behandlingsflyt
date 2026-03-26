package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.register.tiltakspenger

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.gateway.TiltakspengerKilde
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.gateway.TiltakspengerPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.gateway.TiltakspengerYtelseType
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.tiltakspenger.TiltakspengerRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import no.nav.aap.lookup.repository.Factory

class TiltakspengerRepositoryImpl(private val connection: DBConnection) : TiltakspengerRepository {

    companion object : Factory<TiltakspengerRepositoryImpl>{
        override fun konstruer(connection: DBConnection): TiltakspengerRepositoryImpl {
            return TiltakspengerRepositoryImpl(connection)
        }
    }

    override fun lagre(
        behandlingId: BehandlingId,
        tiltakspengerPeriode: List<TiltakspengerPeriode>
    ) {
        val eksisterendeTiltakspengerGrunnlag = hent(behandlingId)

        if (eksisterendeTiltakspengerGrunnlag.isNotEmpty()) {
            deaktiverEksisterende(behandlingId)
        }

        val tpPeriode = connection.executeReturnKey("""INSERT INTO TILTAKSPENGER_PERIODER DEFAULT VALUES""")

        connection.execute(
            """INSERT INTO TILTAKSPENGER_GRUNNLAG (AKTIV, BEHANDLING_ID, PERIODER_ID)
            VALUES (TRUE, ?, ?)"""
        ) {
            setParams {
                setLong(1, behandlingId.id)
                setLong(2, tpPeriode)
            }
        }

        connection.executeBatch("""INSERT INTO TILTAKSPENGER_PERIODE (PERIODER_ID, PERIODE, YTELSE_TYPE, KILDE) 
            VALUES (?, ?::daterange, ?, ?)""".trimIndent(), tiltakspengerPeriode) {
            setParams {periode ->
                setLong(1, tpPeriode)
                setPeriode(2, Periode(periode.fraOgMed, periode.tilOgMed))
                setString(3, periode.tiltakspengerYtelseType.name)
                setString(4, periode.kilde.name)
            }
        }
    }

    override fun slett(behandlingId: BehandlingId) {
        val perioderIds = getPerioderId(behandlingId)

        connection.execute("""
            DELETE FROM TILTAKSPENGER_GRUNNLAG WHERE BEHANDLING_ID = ?;
            DELETE FROM TILTAKSPENGER_PERIODE WHERE PERIODER_ID = ANY(?::BigInt[]);
            DELETE FROM TILTAKSPENGER_PERIODER WHERE ID = ANY(?::BigInt[]);
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
    ) : List<TiltakspengerPeriode>{
        return connection.queryList(
            """SELECT TP.PERIODE, TP.YTELSE_TYPE, TP.KILDE
            FROM TILTAKSPENGER_PERIODE TP
            JOIN TILTAKSPENGER_PERIODER TPER ON TP.PERIODER_ID = TPER.ID
            JOIN TILTAKSPENGER_GRUNNLAG TG ON TPER.ID = TG.PERIODER_ID
            WHERE TG.BEHANDLING_ID = ? AND TG.AKTIV = TRUE""".trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.id)
            }
            setRowMapper { row ->
                TiltakspengerPeriode(
                    fraOgMed = row.getPeriode("PERIODE").fom,
                    tilOgMed = row.getPeriode("PERIODE").tom,
                    tiltakspengerYtelseType = TiltakspengerYtelseType.valueOf(row.getString("YTELSE_TYPE")),
                    kilde = TiltakspengerKilde.valueOf(row.getString("KILDE"))
                )
            }
        }
    }

    private fun deaktiverEksisterende(behandlingId: BehandlingId){
        connection.execute(
            """UPDATE TILTAKSPENGER_GRUNNLAG SET AKTIV = FALSE WHERE BEHANDLING_ID = ? AND AKTIV = TRUE"""
        ) {
            setParams {
                setLong(1, behandlingId.id)
            }
            setResultValidator { rowsUpdated ->
                require(rowsUpdated==1)
            }
        }
    }

    private fun getPerioderId(behandlingId: BehandlingId) : List<Long>{
        return connection.queryList(
            """SELECT PERIODER_ID
            FROM TILTAKSPENGER_GRUNNLAG
            WHERE BEHANDLING_ID = ? AND AKTIV""".trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.id)
            }
            setRowMapper { row ->
                row.getLong("PERIODER_ID")
            }
        }
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        require(fraBehandling != tilBehandling) {
            "Kan ikke kopiere tiltakspengergrunnlag til samme behandling"
        }
        connection.execute("""INSERT INTO TILTAKSPENGER_GRUNNLAG (BEHANDLING_ID, PERIODER_ID) 
                SELECT ?, PERIODER_ID 
                FROM TILTAKSPENGER_GRUNNLAG 
                WHERE AKTIV AND BEHANDLING_ID = ?
            """.trimIndent()) {
            setParams {
                setLong(1, tilBehandling.id)
                setLong(2, fraBehandling.id)
            }
        }
    }
}
