package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument

import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.DokumentRekkefølge
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Status
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.dokumenter.Brevkode
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.verdityper.dokument.JournalpostId
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import no.nav.aap.verdityper.sakogbehandling.SakId

class MottattDokumentRepository(private val connection: DBConnection) {
    fun lagre(mottattDokument: MottattDokument) {
        val query = """
            INSERT INTO MOTTATT_DOKUMENT (sak_id, journalpost, MOTTATT_TID, type, status, strukturert_dokument) VALUES (?, ?, ?, ?, ?, ?)
        """.trimIndent()

        connection.execute(query) {
            setParams {
                setLong(1, mottattDokument.sakId.toLong())
                setString(2, mottattDokument.journalpostId.identifikator)
                setLocalDateTime(3, mottattDokument.mottattTidspunkt)
                setEnumName(4, mottattDokument.type)
                setEnumName(5, mottattDokument.status)
                setString(6, mottattDokument.ustrukturerteData())
            }
        }
    }

    fun oppdaterStatus(journalpostId: JournalpostId, behandlingId: BehandlingId, sakId: SakId, status: Status) {
        val query = """
            UPDATE MOTTATT_DOKUMENT SET behandling_id = ?, status = ? WHERE journalpost = ? AND sak_id = ?
        """.trimIndent()
        connection.execute(query) {
            setParams {
                setLong(1, behandlingId.toLong())
                setEnumName(2, status)
                setString(3, journalpostId.identifikator)
                setLong(4, sakId.toLong())
            }
            setResultValidator {
                require(1 == it)
            }
        }
    }

    fun hentUbehandledeDokumenterAvType(sakId: SakId, dokumentType: Brevkode): Set<MottattDokument> {
        val query = """
            SELECT * FROM MOTTATT_DOKUMENT WHERE sak_id = ? AND status = ? AND type = ?
        """.trimIndent()

        return connection.queryList(query) {
            setParams {
                setLong(1, sakId.toLong())
                setEnumName(2, Status.MOTTATT)
                setEnumName(3, dokumentType)
            }
            setRowMapper { row ->
                mapMottattDokument(row)
            }
        }.toSet()
    }

    private fun mapMottattDokument(row: Row): MottattDokument {
        val journalpostId = JournalpostId(row.getString("journalpost"))
        val brevkode: Brevkode = row.getEnum("type")
        return MottattDokument(
            journalpostId,
            SakId(row.getLong("sak_id")),
            null,
            row.getLocalDateTime("MOTTATT_TID"),
            brevkode,
            row.getEnum("status"),
            LazyStrukturertDokument(journalpostId, brevkode, connection)
        )
    }

    fun hentDokumentRekkefølge(sakId: SakId, type: Brevkode): Set<DokumentRekkefølge> {
        val query = """
            SELECT journalpost, MOTTATT_TID FROM MOTTATT_DOKUMENT WHERE sak_id = ? AND status = ? AND type = ?
        """.trimIndent()

        return connection.queryList(query) {
            setParams {
                setLong(1, sakId.toLong())
                setEnumName(2, Status.BEHANDLET)
                setEnumName(3, type)
            }
            setRowMapper {
                DokumentRekkefølge(
                    JournalpostId(it.getString("journalpost")),
                    it.getLocalDateTime("mottatt_tid")
                )
            }
        }.toSet()
    }

    fun hentDokumenterAvType(sakId: SakId, type: Brevkode): Set<MottattDokument> {
        val query = """
            SELECT * FROM MOTTATT_DOKUMENT WHERE sak_id = ? AND type = ?
        """.trimIndent()

        return connection.queryList(query) {
            setParams {
                setLong(1, sakId.toLong())
                setEnumName(2, type)
            }
            setRowMapper { row ->
                mapMottattDokument(row)
            }
        }.toSet()
    }
}