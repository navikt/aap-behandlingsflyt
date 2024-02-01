package no.nav.aap.behandlingsflyt.faktagrunnlag.arbeid.adapter

import no.nav.aap.verdityper.dokument.JournalpostId
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.arbeid.ArbeidIPeriode
import no.nav.aap.verdityper.TimerArbeid

class MottakAvPliktkortRepository(private val connection: DBConnection) {

    fun lagre(pliktkort: UbehandletPliktkort) {
        val query = """
            INSERT INTO SAK_PLIKTKORT (journalpost) VALUES (?)
        """.trimIndent()

        val pliktkortId = connection.executeReturnKey(query) {
            setParams {
                setString(1, pliktkort.journalpostId.identifikator)
            }
        }

        pliktkort.timerArbeidPerPeriode.forEach { kort ->
            val kortQuery = """
                INSERT INTO SAK_PLIKTKORT_PERIODE (pliktkort_id, periode, timer_arbeid) VALUES (?, ?::daterange, ?)
            """.trimIndent()

            connection.execute(kortQuery) {
                setParams {
                    setLong(1, pliktkortId)
                    setPeriode(2, kort.periode)
                    setBigDecimal(3, kort.timerArbeid.antallTimer)
                }
            }
        }
    }

    fun hent(journalpostIder: Set<JournalpostId>): List<UbehandletPliktkort> {
        val query = """
            SELECT * FROM SAK_PLIKTKORT WHERE journalpost in (?)
        """.trimIndent()
        return connection.queryList(query) {
            setParams {
                setString(1, journalpostIder.joinToString(", ") { it.identifikator })
            }
            setRowMapper {
                UbehandletPliktkort(JournalpostId(it.getString("journalpost")), hentTimerArbeid(it.getLong("id")))
            }
        }
    }

    private fun hentTimerArbeid(long: Long): Set<ArbeidIPeriode> {
        val query = """
            SELECT * FROM SAK_PLIKTKORT_PERIODE WHERE pliktkort_id = ?
        """.trimIndent()
        return connection.queryList(query) {
            setParams {
                setLong(1, long)
            }
            setRowMapper {
                ArbeidIPeriode(it.getPeriode("periode"), TimerArbeid(it.getBigDecimal("timer_arbeid")))
            }
        }.toSet()
    }
}