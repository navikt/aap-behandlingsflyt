package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument

import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.DokumentRekkefølge
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Status
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.dokumenter.Brevkode
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import no.nav.aap.verdityper.sakogbehandling.SakId

class MottattDokumentRepository(private val connection: DBConnection) {
    fun lagre(mottattDokument: MottattDokument) {
        val query = """
            INSERT INTO MOTTATT_DOKUMENT (sak_id, MOTTATT_TID, type, status, strukturert_dokument, referanse, referanse_type) VALUES (?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()

        connection.execute(query) {
            setParams {
                setLong(1, mottattDokument.sakId.toLong())
                setLocalDateTime(2, mottattDokument.mottattTidspunkt)
                setEnumName(3, mottattDokument.type)
                setEnumName(4, mottattDokument.status)
                setString(5, mottattDokument.ustrukturerteData())
                setString(6, mottattDokument.referanse.verdi)
                setEnumName(7, mottattDokument.referanse.type)
            }
        }
    }

    fun oppdaterStatus(dokumentReferanse: MottattDokumentReferanse, behandlingId: BehandlingId, sakId: SakId, status: Status) {
        val query = """
            UPDATE MOTTATT_DOKUMENT SET behandling_id = ?, status = ? WHERE referanse_type = ? AND referanse = ? AND sak_id = ?
        """.trimIndent()
        connection.execute(query) {
            setParams {
                setLong(1, behandlingId.toLong())
                setEnumName(2, status)
                setEnumName(3, dokumentReferanse.type)
                setString(4, dokumentReferanse.verdi)
                setLong(5, sakId.toLong())
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
        val brevkode: Brevkode = row.getEnum("type")
        val referanse = mapDokumentReferanse(row)
        return MottattDokument(
            referanse = referanse,
            sakId = SakId(row.getLong("sak_id")),
            behandlingId = null,
            mottattTidspunkt = row.getLocalDateTime("MOTTATT_TID"),
            type = brevkode,
            status = row.getEnum("status"),
            strukturertDokument = LazyStrukturertDokument(referanse, brevkode, connection),
        )
    }

    private fun mapDokumentReferanse(row: Row) = MottattDokumentReferanse(
        type = row.getEnum<MottattDokumentReferanse.Type>("referanse_type"),
        verdi = row.getString("referanse")
    )

    fun hentDokumentRekkefølge(sakId: SakId, type: Brevkode): Set<DokumentRekkefølge> {
        val query = """
            SELECT referanse, referanse_type, MOTTATT_TID FROM MOTTATT_DOKUMENT WHERE sak_id = ? AND status = ? AND type = ?
        """.trimIndent()

        return connection.queryList(query) {
            setParams {
                setLong(1, sakId.toLong())
                setEnumName(2, Status.BEHANDLET)
                setEnumName(3, type)
            }
            setRowMapper {
                DokumentRekkefølge(
                    mapDokumentReferanse(it),
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