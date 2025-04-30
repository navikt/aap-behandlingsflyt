package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument

import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.DokumentRekkefølge
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Status
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.lookup.repository.Factory

class MottattDokumentRepositoryImpl(private val connection: DBConnection) : MottattDokumentRepository {

    companion object : Factory<MottattDokumentRepositoryImpl> {
        override fun konstruer(connection: DBConnection): MottattDokumentRepositoryImpl {
            return MottattDokumentRepositoryImpl(connection)
        }
    }

    override fun lagre(mottattDokument: MottattDokument) {
        val query = """
            INSERT INTO MOTTATT_DOKUMENT (sak_id, MOTTATT_TID, type, status, strukturert_dokument, referanse,
                                          referanse_type, behandling_id, kanal)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
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
                setLong(8, mottattDokument.behandlingId?.toLong())
                setEnumName(9, mottattDokument.kanal)
            }
        }
    }

    override fun oppdaterStatus(
        dokumentReferanse: InnsendingReferanse,
        behandlingId: BehandlingId,
        sakId: SakId,
        status: Status
    ) {
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

    override fun hentUbehandledeDokumenterAvType(sakId: SakId, dokumentType: InnsendingType): Set<MottattDokument> {
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

    override fun slett(behandlingId: BehandlingId) {
        connection.execute("""
            delete from mottatt_dokument where behandling_id = ?;
        """.trimIndent()) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
        }
    }

    private fun mapMottattDokument(row: Row): MottattDokument {
        val brevkategori: InnsendingType = row.getEnum("type")
        val referanse = mapDokumentReferanse(row)
        return MottattDokument(
            referanse = referanse,
            sakId = SakId(row.getLong("sak_id")),
            behandlingId = row.getLongOrNull("BEHANDLING_ID")?.let { BehandlingId(it) },
            mottattTidspunkt = row.getLocalDateTime("MOTTATT_TID"),
            type = brevkategori,
            kanal = row.getEnum("kanal"),
            status = row.getEnum("status"),
            strukturertDokument = LazyStrukturertDokument(referanse, connection),
        )
    }

    private fun mapDokumentReferanse(row: Row) = InnsendingReferanse(
        type = row.getEnum<InnsendingReferanse.Type>("referanse_type"),
        verdi = row.getString("referanse")
    )

    override fun hentDokumentRekkefølge(sakId: SakId, type: InnsendingType): Set<DokumentRekkefølge> {
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

    override fun hentDokumenterAvType(sakId: SakId, type: InnsendingType): Set<MottattDokument> {
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

    override fun hentDokumenterAvType(behandlingId: BehandlingId, type: InnsendingType): Set<MottattDokument> {
        val query = """
            SELECT * FROM MOTTATT_DOKUMENT
            WHERE behandling_id = ? AND type = ?
        """.trimIndent()

        return connection.queryList(query) {
            setParams {
                setLong(1, behandlingId.toLong())
                setEnumName(2, type)
            }
            setRowMapper { row ->
                mapMottattDokument(row)
            }
        }.toSet()
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        // Denne trengs ikke implementeres
    }
}