package no.nav.aap.behandlingsflyt.faktagrunnlag.arbeid

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.dbconnect.Row
import no.nav.aap.verdityper.TimerArbeid
import no.nav.aap.verdityper.dokument.JournalpostId
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import no.nav.aap.verdityper.sakogbehandling.SakId

class PliktkortRepository(private val connection: DBConnection) {

    private val mottattDokumentRepository = MottattDokumentRepository(connection)

    fun hent(behandlingId: BehandlingId): PliktkortGrunnlag {
        return requireNotNull(hentHvisEksisterer(behandlingId))
    }

    fun hentHvisEksisterer(behandlingId: BehandlingId): PliktkortGrunnlag? {
        val query = """
            SELECT * FROM PLIKKORT_GRUNNLAG WHERE behandling_id = ? and aktiv = true
        """.trimIndent()
        return connection.queryFirstOrNull(query) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper {
                mapGrunnlag(it, behandlingId)
            }
        }
    }

    private fun mapGrunnlag(row: Row, behandlingId: BehandlingId): PliktkortGrunnlag {
        val sakId = hentSakId(behandlingId)
        val pliktkorteneId = row.getLong("pliktkortene_id")

        val query = """
            SELECT * FROM PLIKTKORT WHERE pliktkortene_id = ?
        """.trimIndent()

        val pliktkortene = connection.queryList(query) {
            setParams {
                setLong(1, pliktkorteneId)
            }
            setRowMapper {
                Pliktkort(JournalpostId(it.getString("journalpost")), hentTimerPerPeriode(it.getLong("id")))
            }
        }.toSet()

        val dokumentRekkefølge = mottattDokumentRepository.hentDokumentRekkefølge(sakId, DokumentType.PLIKTKORT)

        return PliktkortGrunnlag(pliktkortene, dokumentRekkefølge)
    }


    private fun hentSakId(behandlingId: BehandlingId): SakId {
        val query = """
            SELECT sak_id FROM BEHANDLING WHERE id = ?
        """.trimIndent()

        return connection.queryFirst(query) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper {
                SakId(it.getLong("sak_id"))
            }
        }
    }

    private fun hentTimerPerPeriode(id: Long): Set<ArbeidIPeriode> {
        val query = """
            SELECT * FROM PLIKTKORT_PERIODE WHERE pliktkort_id = ?
        """.trimIndent()

        return connection.queryList(query) {
            setParams {
                setLong(1, id)
            }
            setRowMapper {
                ArbeidIPeriode(it.getPeriode("periode"), TimerArbeid(it.getBigDecimal("timer_arbeid")))
            }
        }.toSet()
    }

    fun lagre(behandlingId: BehandlingId, pliktkortene: Set<Pliktkort>) {
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)
        val eksisterendeKort = eksisterendeGrunnlag?.pliktkortene ?: emptySet()

        if (eksisterendeKort != pliktkortene) {
            if (eksisterendeGrunnlag != null) {
                deaktiverGrunnlag(behandlingId)
            }

            lagreNyttGrunnlag(behandlingId, pliktkortene)
        }
    }

    private fun lagreNyttGrunnlag(behandlingId: BehandlingId, pliktkortene: Set<Pliktkort>) {
        val pliktkorteneQuery = """
            INSERT INTO PLIKTKORTENE DEFAULT VALUES
            """.trimIndent()
        val pliktkorteneId = connection.executeReturnKey(pliktkorteneQuery)

        pliktkortene.forEach { pliktkort ->
            val query = """
            INSERT INTO PLIKTKORT (journalpost, pliktkortene_id) VALUES (?, ?)
            """.trimIndent()
            val pliktkortId = connection.executeReturnKey(query) {
                setParams {
                    setString(1, pliktkort.journalpostId.identifikator)
                    setLong(2, pliktkorteneId)
                }
            }

            val kortQuery = """
                INSERT INTO PLIKTKORT_PERIODE (pliktkort_id, periode, timer_arbeid) VALUES (?, ?::daterange, ?)
            """.trimIndent()

            connection.executeBatch(kortQuery, pliktkort.timerArbeidPerPeriode) {
                setParams { periode ->
                    setLong(1, pliktkortId)
                    setPeriode(2, periode.periode)
                    setBigDecimal(3, periode.timerArbeid.antallTimer)
                }
            }
        }

        val grunnlagQuery = """
            INSERT INTO PLIKKORT_GRUNNLAG (behandling_id, PLIKTKORTENE_ID) VALUES (?, ?)
        """.trimIndent()
        connection.execute(grunnlagQuery) {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, pliktkorteneId)
            }
        }
    }

    private fun deaktiverGrunnlag(behandlingId: BehandlingId) {
        connection.execute("UPDATE PLIKKORT_GRUNNLAG set aktiv = false WHERE behandling_id = ? and aktiv = true") {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setResultValidator { require(it == 1) }
        }
    }

    fun kopier(fraBehandlingId: BehandlingId, tilBehandlingId: BehandlingId) {
        val eksisterendeGrunnlag = hentHvisEksisterer(fraBehandlingId)
        if (eksisterendeGrunnlag == null) {
            return
        }
        val query = """
            INSERT INTO PLIKKORT_GRUNNLAG (behandling_id, pliktkortene_id) SELECT ?, pliktkortene_id from PLIKKORT_GRUNNLAG where behandling_id = ? and aktiv
        """.trimIndent()

        connection.execute(query) {
            setParams {
                setLong(1, tilBehandlingId.toLong())
                setLong(2, fraBehandlingId.toLong())
            }
        }
    }
}