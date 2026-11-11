package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.sykdom

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdomsvurderingbrev.Dokumentkilde
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdomsvurderingbrev.SykdomsvurderingForBrev
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdomsvurderingbrev.SykdomsvurderingForBrevRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.lookup.repository.Factory
import java.time.LocalDateTime

class SykdomsvurderingForBrevRepositoryImpl(private val connection: DBConnection) : SykdomsvurderingForBrevRepository {

    override fun lagre(
        behandlingId: BehandlingId,
        vurdering: SykdomsvurderingForBrev
    ) {
        val eksisterende = hent(behandlingId)
        if (eksisterende != null) {
            deaktiverEksisterende(behandlingId)
        }

        val query = """
            INSERT INTO SYKDOM_VURDERING_BREV (behandling_id, vurdering, vurdert_av, opprettet_tid, aktiv) 
            VALUES (?, ?, ?, ?, true);
        """.trimIndent()

        val vurderingId = connection.executeReturnKey(query) {
            setParams {
                setLong(1, behandlingId.toLong())
                setString(2, vurdering.vurdering)
                setBruker(3, vurdering.vurdertAv)
                setLocalDateTime(4, vurdering.vurdertTidspunkt)
            }
        }

        connection.executeBatch(
            """
                INSERT INTO SYKDOM_VURDERING_BREV_KILDE
                    (sykdom_vurdering_brev_id, journalpost_id, dokument_info_id, dokumentnavn, journalforingsdato)
                VALUES (?, ?, ?, ?, ?)
            """.trimIndent(),
            vurdering.kilder
        ) {
            setParams { kilde ->
                setLong(1, vurderingId)
                setString(2, kilde.journalpostId)
                setString(3, kilde.dokumentInfoId)
                setString(4, kilde.dokumentnavn)
                setLocalDate(5, kilde.journalføringsdato)
            }
        }
    }

    override fun deaktiverEksisterende(behandlingId: BehandlingId) {
        connection.execute("UPDATE SYKDOM_VURDERING_BREV SET aktiv = false WHERE behandling_id = ? AND aktiv = true") {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setResultValidator { rowsUpdated ->
                require(rowsUpdated <= 1) { "Mindre enn én rad skal oppdateres, men oppdaterte $rowsUpdated. BehandlingId: $behandlingId."}
            }
        }
    }

    override fun hent(behandlingId: BehandlingId): SykdomsvurderingForBrev? {
        val query = """
            SELECT * FROM SYKDOM_VURDERING_BREV WHERE behandling_id = ? AND aktiv = true;
        """.trimIndent()

        return connection.queryFirstOrNull(query) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper { row ->
                toSykdomsvurderingForBrev(row)
            }
        }
    }

    override fun hentSykdomsvurderingForBrevPåTidspunkt(
        behandlingId: BehandlingId,
        tidspunkt: LocalDateTime
    ): SykdomsvurderingForBrev? {
        val query = """
            SELECT * FROM SYKDOM_VURDERING_BREV
        WHERE behandling_id = ?
        AND opprettet_tid <= ?
        ORDER BY opprettet_tid DESC, id DESC
        LIMIT 1
        """.trimIndent()
        return connection.queryFirstOrNull(query) {
            setParams {
                setLong(1, behandlingId.toLong())
                setLocalDateTime(2, tidspunkt)
            }
            setRowMapper { row ->
                toSykdomsvurderingForBrev(row)
            }
        }
    }

    override fun hent(sakId: SakId): List<SykdomsvurderingForBrev> {
        val query = """
            SELECT s.* FROM SYKDOM_VURDERING_BREV s
            JOIN BEHANDLING b ON s.behandling_id = b.id
            LEFT JOIN AVBRYT_REVURDERING_GRUNNLAG ar ON ar.BEHANDLING_ID = b.ID 
            WHERE sak_id = ?
            AND s.aktiv = true
            AND ar.BEHANDLING_ID IS NULL
        """.trimIndent()

        return connection.queryList(query) {
            setParams {
                setLong(1, sakId.toLong())
            }
            setRowMapper { row ->
                toSykdomsvurderingForBrev(row)
            }
        }
    }

    private fun toSykdomsvurderingForBrev(row: Row): SykdomsvurderingForBrev = SykdomsvurderingForBrev(
        behandlingId = BehandlingId(row.getLong("BEHANDLING_ID")),
        vurdering = row.getStringOrNull("VURDERING"),
        vurdertAv = row.getBruker("VURDERT_AV"),
        vurdertTidspunkt = row.getLocalDateTime("OPPRETTET_TID"),
        kilder = hentKilder(row.getLong("ID")),
    )

    private fun hentKilder(vurderingId: Long): List<Dokumentkilde> = connection.queryList(
        """
            SELECT journalpost_id, dokument_info_id, dokumentnavn, journalforingsdato
            FROM SYKDOM_VURDERING_BREV_KILDE
            WHERE sykdom_vurdering_brev_id = ?
            ORDER BY id
        """.trimIndent()
    ) {
        setParams {
            setLong(1, vurderingId)
        }
        setRowMapper { row ->
            Dokumentkilde(
                journalpostId = row.getString("journalpost_id"),
                dokumentInfoId = row.getString("dokument_info_id"),
                dokumentnavn = row.getString("dokumentnavn"),
                journalføringsdato = row.getLocalDate("journalforingsdato"),
            )
        }
    }

    override fun kopier(
        fraBehandling: BehandlingId,
        tilBehandling: BehandlingId
    ) {
        // Skal ikke kopieres ved revurdering da en vurdering for brev er spesifikk for en behandling
    }

    override fun slett(behandlingId: BehandlingId) {
        val query = """
            DELETE FROM SYKDOM_VURDERING_BREV WHERE behandling_id = ?
        """.trimIndent()

        connection.execute(query) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
        }
    }

    companion object : Factory<SykdomsvurderingForBrevRepositoryImpl> {
        override fun konstruer(connection: DBConnection): SykdomsvurderingForBrevRepositoryImpl {
            return SykdomsvurderingForBrevRepositoryImpl(connection)
        }
    }

}