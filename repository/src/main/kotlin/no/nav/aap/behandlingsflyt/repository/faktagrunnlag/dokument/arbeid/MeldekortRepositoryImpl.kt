package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.dokument.arbeid

import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepositoryImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.ArbeidIPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Meldekort
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.MeldekortGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.MeldekortRepository
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.komponenter.verdityper.TimerArbeid
import no.nav.aap.lookup.repository.Factory
import no.nav.aap.verdityper.dokument.JournalpostId
import org.slf4j.LoggerFactory

class MeldekortRepositoryImpl(private val connection: DBConnection) : MeldekortRepository {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object : Factory<MeldekortRepositoryImpl> {
        override fun konstruer(connection: DBConnection): MeldekortRepositoryImpl {
            return MeldekortRepositoryImpl(connection)
        }
    }

    // Ekte impl her, siden dette er inni en ekte impl
    private val mottattDokumentRepository = MottattDokumentRepositoryImpl(connection)

    override fun hent(behandlingId: BehandlingId): MeldekortGrunnlag {
        return requireNotNull(hentHvisEksisterer(behandlingId))
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): MeldekortGrunnlag? {
        val query = """
            SELECT * FROM MELDEKORT_GRUNNLAG WHERE behandling_id = ? and aktiv = true
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

    private fun mapGrunnlag(row: Row, behandlingId: BehandlingId): MeldekortGrunnlag {
        val sakId = hentSakId(behandlingId)
        val meldekorteneId = row.getLong("meldekortene_id")

        val query = """
            SELECT * FROM MELDEKORT WHERE meldekortene_id = ?
        """.trimIndent()

        val meldekortene = connection.queryList(query) {
            setParams {
                setLong(1, meldekorteneId)
            }
            setRowMapper {
                Meldekort(JournalpostId(it.getString("journalpost")), hentTimerPerPeriode(it.getLong("id")), mottattTidspunkt = it.getLocalDateTime("mottatt_tidspunkt"))
            }
        }.toSet()

        val dokumentRekkefølge = mottattDokumentRepository.hentDokumentRekkefølge(sakId, InnsendingType.MELDEKORT)

        return MeldekortGrunnlag(meldekortene, dokumentRekkefølge)
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
            SELECT * FROM MELDEKORT_PERIODE WHERE meldekort_id = ?
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

    override fun lagre(behandlingId: BehandlingId, meldekortene: Set<Meldekort>) {
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)
        val eksisterendeKort = eksisterendeGrunnlag?.meldekort()?.toSet() ?: emptySet()

        if (eksisterendeKort != meldekortene) {
            if (eksisterendeGrunnlag != null) {
                deaktiverGrunnlag(behandlingId)
            }

            lagreNyttGrunnlag(behandlingId, meldekortene)
        }
    }

    private fun lagreNyttGrunnlag(behandlingId: BehandlingId, meldekortene: Set<Meldekort>) {
        val meldekorteneQuery = """
            INSERT INTO MELDEKORTENE DEFAULT VALUES
            """.trimIndent()
        val meldekorteneId = connection.executeReturnKey(meldekorteneQuery)

        meldekortene.forEach { meldekort ->
            val query = """
            INSERT INTO MELDEKORT (journalpost, meldekortene_id, mottatt_tidspunkt) VALUES (?, ?, ?)
            """.trimIndent()
            val meldekortId = connection.executeReturnKey(query) {
                setParams {
                    setString(1, meldekort.journalpostId.identifikator)
                    setLong(2, meldekorteneId)
                    setLocalDateTime(3, meldekort.mottattTidspunkt)
                }
            }

            val kortQuery = """
                INSERT INTO MELDEKORT_PERIODE (meldekort_id, periode, timer_arbeid) VALUES (?, ?::daterange, ?)
            """.trimIndent()

            connection.executeBatch(kortQuery, meldekort.timerArbeidPerPeriode) {
                setParams { periode ->
                    setLong(1, meldekortId)
                    setPeriode(2, periode.periode)
                    setBigDecimal(3, periode.timerArbeid.antallTimer)
                }
            }
        }

        val grunnlagQuery = """
            INSERT INTO MELDEKORT_GRUNNLAG (behandling_id, MELDEKORTENE_ID) VALUES (?, ?)
        """.trimIndent()
        connection.execute(grunnlagQuery) {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, meldekorteneId)
            }
        }
    }

    private fun deaktiverGrunnlag(behandlingId: BehandlingId) {
        connection.execute("UPDATE MELDEKORT_GRUNNLAG set aktiv = false WHERE behandling_id = ? and aktiv = true") {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setResultValidator { require(it == 1) }
        }
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        val eksisterendeGrunnlag = hentHvisEksisterer(fraBehandling)
        if (eksisterendeGrunnlag == null) {
            return
        }
        val query = """
            INSERT INTO MELDEKORT_GRUNNLAG (behandling_id, meldekortene_id) SELECT ?, meldekortene_id from MELDEKORT_GRUNNLAG where behandling_id = ? and aktiv
        """.trimIndent()

        connection.execute(query) {
            setParams {
                setLong(1, tilBehandling.toLong())
                setLong(2, fraBehandling.toLong())
            }
        }
    }

    override fun slett(behandlingId: BehandlingId) {

        val meldekorteneIds = getMeldekorteneIds(behandlingId)
        val meldekortIds = getMeldekortIds(meldekorteneIds)
        val deletedRows = connection.executeReturnUpdated("""
            delete from meldekort_grunnlag where behandling_id = ?; 
            delete from meldekort_periode where meldekort_id = ANY(?::bigint[]);
            delete from meldekort where meldekortene_id = ANY(?::bigint[]);
            delete from meldekortene where id = ANY(?::bigint[]);
        """.trimIndent()) {
            setParams {
                setLong(1, behandlingId.id)
                setLongArray(2, meldekortIds)
                setLongArray(3, meldekorteneIds)
                setLongArray(4, meldekorteneIds)
            }
        }
        log.info("Slettet $deletedRows raderfra meldekort_grunnlag")
    }

    private fun getMeldekorteneIds(behandlingId: BehandlingId): List<Long> = connection.queryList(
        """
                    SELECT meldekortene_id
                    FROM meldekort_grunnlag
                    WHERE behandling_id = ? AND meldekortene_id is not null
                 
                """.trimIndent()
    ) {
        setParams { setLong(1, behandlingId.id) }
        setRowMapper { row ->
            row.getLong("meldekortene_id")
        }
    }

    private fun getMeldekortIds(meldekorteneId: List<Long>): List<Long> = connection.queryList(
        """
                    SELECT id
                    FROM meldekort
                    WHERE meldekortene_id = ANY(?::bigint[])
                 
                """.trimIndent()
    ) {
        setParams { setLongArray(1, meldekorteneId) }
        setRowMapper { row ->
            row.getLong("id")
        }
    }

}