package no.nav.aap.behandlingsflyt.tilkjentytelse

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.Tilkjent
import no.nav.aap.tidslinje.Segment
import no.nav.aap.tidslinje.Tidslinje
import no.nav.aap.verdityper.Periode
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

class TilkjentYtelseRepository(private val connection: DBConnection) {
    fun hentHvisEksiterer(behandlingId: BehandlingId): Tidslinje<Tilkjent>? {
        val tilkjent = connection.queryList(
            """
            SELECT * FROM TILKJENT_PERIODE WHERE TILKJENT_YTELSE_ID IN (SELECT ID FROM TILKJENT_YTELSE WHERE BEHANDLING_ID=? AND AKTIV=TRUE)
        """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper {
                Segment(
                    periode = it.getPeriode("PERIODE"),
                    Tilkjent(
                        dagsats = no.nav.aap.verdityper.Beløp(it.getInt("DAGSATS")),
                        gradering = no.nav.aap.verdityper.Prosent(it.getInt("GRADERING")),
                        barnetillegg = no.nav.aap.verdityper.Beløp(it.getInt("BARNETILLEGG")),
                        grunnlagsfaktor = no.nav.aap.verdityper.GUnit(it.getBigDecimal("GRUNNLAGSFAKTOR")),
                        grunnlag = no.nav.aap.verdityper.Beløp(it.getInt("GRUNNLAG")),
                        antallBarn = it.getInt("ANTALL_BARN"),
                        barnetilleggsats = no.nav.aap.verdityper.Beløp(it.getInt("BARNETILLEGGSATS")),
                        grunnbeløp = no.nav.aap.verdityper.Beløp(it.getInt("GRUNNBELOP")),
                        )
                )
            }
        }
        if(tilkjent.isEmpty()) {
            return null
        }
        return Tidslinje(tilkjent)
    }

    fun lagre(behandlingId: BehandlingId, tilkjent: Tidslinje<Tilkjent>) {
        val eksisterendeTilkjent = hentHvisEksiterer(behandlingId)
        if(eksisterendeTilkjent == tilkjent) {
            return
        }

        if(eksisterendeTilkjent!=null) {
            deaktiverEksisterende(behandlingId)
        }

        val tilkjentYtelseKey = connection.executeReturnKey("""
            INSERT INTO TILKJENT_YTELSE (BEHANDLING_ID, AKTIV) VALUES (?, TRUE)
        """.trimIndent()) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
        }
        tilkjent.forEach { segment ->
            lagrePeriode(tilkjentYtelseKey, segment.periode, segment.verdi)
        }

    }

    private fun lagrePeriode(tilkjentYtelseId: Long, periode: Periode, tilkjent: Tilkjent) {
        connection.execute("""
            INSERT INTO TILKJENT_PERIODE (TILKJENT_YTELSE_ID, PERIODE, DAGSATS, GRADERING, BARNETILLEGG, GRUNNLAGSFAKTOR, GRUNNLAG, ANTALL_BARN, BARNETILLEGGSATS, GRUNNBELOP) VALUES (?, ?::daterange, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()) {
            setParams {
                setLong(1, tilkjentYtelseId)
                setPeriode(2, periode)
                setBigDecimal(3, tilkjent.dagsats.verdi())
                setInt(4, tilkjent.gradering.prosentverdi())
                setBigDecimal(5, tilkjent.barnetillegg.verdi())
                setBigDecimal(6, tilkjent.grunnlagsfaktor.verdi())
                setBigDecimal(7, tilkjent.grunnlag.verdi())
                setInt(8, tilkjent.antallBarn)
                setBigDecimal(9, tilkjent.barnetilleggsats.verdi())
                setBigDecimal(10, tilkjent.grunnbeløp.verdi())
            }
        }
    }

    fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        require(fraBehandling != tilBehandling)
    }

    private fun deaktiverEksisterende(behandlingId: BehandlingId) {
        connection.execute("UPDATE TILKJENT_YTELSE SET AKTIV = FALSE WHERE AKTIV AND BEHANDLING_ID = ?") {
            setParams {
                setLong(1, behandlingId.toLong())
            }
        }
    }

}