package no.nav.aap.behandlingsflyt.repository.behandling.tilkjentytelse

import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.Tilkjent
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentGradering
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelsePeriode
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelseRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.GUnit
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.lookup.repository.Factory
import org.slf4j.LoggerFactory

class TilkjentYtelseRepositoryImpl(private val connection: DBConnection) :
    TilkjentYtelseRepository {

    private val log = LoggerFactory.getLogger(javaClass)
    companion object : Factory<TilkjentYtelseRepositoryImpl> {
        override fun konstruer(connection: DBConnection): TilkjentYtelseRepositoryImpl {
            return TilkjentYtelseRepositoryImpl(connection)
        }
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): List<TilkjentYtelsePeriode>? {
        val tilkjent = connection.queryList(
            """
            SELECT * FROM TILKJENT_PERIODE WHERE TILKJENT_YTELSE_ID IN (SELECT ID FROM TILKJENT_YTELSE WHERE BEHANDLING_ID=? AND AKTIV=TRUE)
        """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper {
                TilkjentYtelsePeriode(
                    periode = it.getPeriode("PERIODE"),
                    Tilkjent(
                        dagsats = Beløp(it.getInt("DAGSATS")),
                        gradering = TilkjentGradering(
                            samordningUføregradering = it.getIntOrNull("SAMORDNING_UFORE_GRADERING")?.let { result -> Prosent(result) },
                            samordningGradering = it.getIntOrNull("SAMORDNING_GRADERING")?.let { result -> Prosent(result) },
                            institusjonGradering = it.getIntOrNull("INSTITUSJON_GRADERING")?.let { result -> Prosent(result) },
                            arbeidGradering = it.getIntOrNull("ARBEID_GRADERING")?.let { result -> Prosent(result) },
                            endeligGradering = Prosent(it.getInt("GRADERING"))
                        ),
                        barnetillegg = Beløp(it.getInt("BARNETILLEGG")),
                        grunnlagsfaktor = GUnit(it.getBigDecimal("GRUNNLAGSFAKTOR")),
                        grunnlag = Beløp(it.getInt("GRUNNLAG")),
                        antallBarn = it.getInt("ANTALL_BARN"),
                        barnetilleggsats = Beløp(it.getInt("BARNETILLEGGSATS")),
                        grunnbeløp = Beløp(it.getInt("GRUNNBELOP")),
                        utbetalingsdato = it.getLocalDate("UTBETALINGSDATO"),
                        meldeperiode = it.getPeriode("meldeperiode")
                    )
                )
            }
        }
        if (tilkjent.isEmpty()) {
            return null
        }
        return tilkjent
    }

    override fun lagre(behandlingId: BehandlingId, tilkjent: List<TilkjentYtelsePeriode>) {
        val eksisterendeTilkjent = hentHvisEksisterer(behandlingId)
        if (eksisterendeTilkjent == tilkjent) {
            return
        }

        if (eksisterendeTilkjent != null) {
            deaktiverEksisterende(behandlingId)
        }

        val tilkjentYtelseKey = connection.executeReturnKey(
            """
            INSERT INTO TILKJENT_YTELSE (BEHANDLING_ID, AKTIV) VALUES (?, TRUE)
        """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
        }
        tilkjent.forEach { segment ->
            lagrePeriode(tilkjentYtelseKey, segment.periode, segment.tilkjent)
        }

    }

    override fun slett(behandlingId: BehandlingId) {
        val deletedRows = connection.executeReturnUpdated("""
            delete from tilkjent_periode where tilkjent_ytelse_id in (select tilkjent_ytelse.id from tilkjent_ytelse where behandling_id = ?);
            delete from tilkjent_ytelse where behandling_id = ? 
        """.trimIndent()) {
            setParams {
                setLong(1, behandlingId.id)
                setLong(2, behandlingId.id)
            }
        }
        log.info("Slettet $deletedRows rader fra tilkjent_periode")
    }

    private fun lagrePeriode(tilkjentYtelseId: Long, periode: Periode, tilkjent: Tilkjent) {
        connection.execute(
            """
            INSERT INTO TILKJENT_PERIODE (TILKJENT_YTELSE_ID, PERIODE, DAGSATS, GRADERING, BARNETILLEGG,
                                          GRUNNLAGSFAKTOR, GRUNNLAG, ANTALL_BARN, BARNETILLEGGSATS, GRUNNBELOP, 
                                          UTBETALINGSDATO, SAMORDNING_GRADERING, INSTITUSJON_GRADERING, ARBEID_GRADERING, SAMORDNING_UFORE_GRADERING, meldeperiode)
            VALUES (?, ?::daterange, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::daterange)
        """.trimIndent()
        ) {
            setParams {
                setLong(1, tilkjentYtelseId)
                setPeriode(2, periode)
                setBigDecimal(3, tilkjent.dagsats.verdi())
                setInt(4, tilkjent.gradering.endeligGradering.prosentverdi())
                setBigDecimal(5, tilkjent.barnetillegg.verdi())
                setBigDecimal(6, tilkjent.grunnlagsfaktor.verdi())
                setBigDecimal(7, tilkjent.grunnlag.verdi())
                setInt(8, tilkjent.antallBarn)
                setBigDecimal(9, tilkjent.barnetilleggsats.verdi())
                setBigDecimal(10, tilkjent.grunnbeløp.verdi())
                setLocalDate(11, tilkjent.utbetalingsdato)
                setInt(12, tilkjent.gradering.samordningGradering?.prosentverdi())
                setInt(13, tilkjent.gradering.institusjonGradering?.prosentverdi())
                setInt(14, tilkjent.gradering.arbeidGradering?.prosentverdi())
                setInt(15, tilkjent.gradering.samordningUføregradering?.prosentverdi())
                setPeriode(16, tilkjent.meldeperiode)
            }
        }
    }

    private fun deaktiverEksisterende(behandlingId: BehandlingId) {
        connection.execute("UPDATE TILKJENT_YTELSE SET AKTIV = FALSE WHERE AKTIV AND BEHANDLING_ID = ?") {
            setParams {
                setLong(1, behandlingId.id)
            }
        }
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        val eksisterendeGrunnlag = hentHvisEksisterer(fraBehandling)
        if (eksisterendeGrunnlag == null) {
            return
        }

        lagre(tilBehandling, eksisterendeGrunnlag)
    }
}