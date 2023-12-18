package no.nav.aap.behandlingsflyt.mottak.pliktkort

import no.nav.aap.behandlingsflyt.behandling.dokumenter.JournalpostId
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection

class MottakAvPliktkortRepository(private val connection: DBConnection) {

    fun lagre(pliktkort: UbehandletPliktkort) {
        val query = """
            INSERT INTO SAK_PLIKTKORT (JOURNALPOST_ID) VALUES (?)
        """.trimIndent()

        val pliktkortId = connection.executeReturnKey(query) {
            setParams {
                setString(1, pliktkort.journalpostId.identifikator)
            }
        }

        pliktkort.timerArbeidPerPeriode.forEach { kort ->
            val kortQuery = """
                INSERT INTO SAK_PLIKTKORT_PERIODE (pliktkort_id, fom, tom, timer_arbeid) VALUES (?, ?, ?, ?)
            """.trimIndent()

            connection.execute(kortQuery) {
                setParams {
                    setLong(1, pliktkortId)
                    setLocalDate(2, kort.periode.fom)
                    setLocalDate(3, kort.periode.tom)
                    setBigDecimal(4, kort.timerArbeid.antallTimer)
                }
            }
        }
    }

    fun hent(journalpostIder: Set<JournalpostId>): List<UbehandletPliktkort> {
        val query = """
            SELECT * FROM SAK_PLIKTKORT WHERE JOURNALPOST_ID in (?)
        """.trimIndent()
        return connection.queryList(query) {
            setParams {
                setString(1, journalpostIder.joinToString(", "))
            }
        }
    }
}