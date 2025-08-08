package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.sykdom

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdomsvurderingbrev.SykdomsvurderingForBrev
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdomsvurderingbrev.SykdomsvurderingForBrevRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.lookup.repository.Factory

class SykdomsvurderingForBrevRepositoryImpl(private val connection: DBConnection) : SykdomsvurderingForBrevRepository {

    override fun lagre(
        behandlingId: BehandlingId,
        vurdering: SykdomsvurderingForBrev
    ): SykdomsvurderingForBrev {
        val eksisterende = hent(behandlingId)
        if (eksisterende != null) {
            deaktiverEksisterende(behandlingId)
        }

        val query = """
            INSERT INTO SYKDOM_VURDERING_BREV (behandling_id, vurdering, vurdert_av, opprettet_tid, aktiv) 
            VALUES (?, ?, ?, ?, true);
        """.trimIndent()

        connection.execute(query) {
            setParams {
                setLong(1, behandlingId.toLong())
                setString(2, vurdering.vurdering)
                setString(3, vurdering.vurdertAv)
                setLocalDateTime(4, vurdering.vurdertTidspunkt)
            }
            setResultValidator { rowsUpdated ->
                require(rowsUpdated == 1)
            }
        }
        return requireNotNull(hent(behandlingId)) { "Fant ikke forventet SykdomsvurderingForBrev" }
    }

    private fun deaktiverEksisterende(behandlingId: BehandlingId) {
        connection.execute("UPDATE SYKDOM_VURDERING_BREV SET aktiv = false WHERE behandling_id = ? AND aktiv = true") {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setResultValidator { rowsUpdated ->
                require(rowsUpdated == 1)
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

    override fun hent(sakId: SakId): List<SykdomsvurderingForBrev> {
        val query = """
            SELECT * FROM SYKDOM_VURDERING_BREV s 
            JOIN BEHANDLING b ON s.behandling_id = b.id 
            WHERE sak_id = ?
            AND s.aktiv = true
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
        vurdertAv = row.getString("VURDERT_AV"),
        vurdertTidspunkt = row.getLocalDateTime("OPPRETTET_TID"),
    )

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