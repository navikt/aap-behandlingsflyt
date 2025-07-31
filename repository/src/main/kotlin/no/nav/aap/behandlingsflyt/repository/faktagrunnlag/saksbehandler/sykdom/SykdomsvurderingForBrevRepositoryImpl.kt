package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.sykdom

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdomsvurderingbrev.SykdomsvurderingForBrev
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdomsvurderingbrev.SykdomsvurderingForBrevRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.Factory

class SykdomsvurderingForBrevRepositoryImpl(private val connection: DBConnection) : SykdomsvurderingForBrevRepository {

    override fun lagre(
        behandlingId: BehandlingId,
        vurdering: SykdomsvurderingForBrev
    ) {
        val query = """
            INSERT INTO SYKDOM_VURDERING_BREV (BEHANDLING_ID, VURDERING, VURDERT_AV) 
            VALUES (?, ?, ?)
            ON CONFLICT (BEHANDLING_ID)
            DO UPDATE SET
              VURDERING = EXCLUDED.VURDERING,
              VURDERT_AV = EXCLUDED.VURDERT_AV;
        """.trimIndent()

        connection.execute(query) {
            setParams {
                setLong(1, behandlingId.toLong())
                setString(2, vurdering.vurdering)
                setString(3, vurdering.vurdertAv)
            }
            setResultValidator { require(it == 1) }
        }
    }

    override fun hent(behandlingId: BehandlingId): SykdomsvurderingForBrev? {
        val query = """
            SELECT * FROM SYKDOM_VURDERING_BREV WHERE behandling_id = ?
        """.trimIndent()

        return connection.queryFirstOrNull(query) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper { row ->
                SykdomsvurderingForBrev(
                    behandlingId = BehandlingId(row.getLong("BEHANDLING_ID")),
                    vurdering = row.getString("VURDERING"),
                    vurdertAv = row.getString("VURDERT_AV"),
                    vurdertTidspunkt = row.getLocalDateTime("OPPRETTET_TID")
                )
            }
        }
    }

    override fun kopier(
        fraBehandling: BehandlingId,
        tilBehandling: BehandlingId
    ) {
        val sykdomsvurderingForBrevForrigeBehandling = hent(behandlingId = fraBehandling)
        if (sykdomsvurderingForBrevForrigeBehandling != null) {
            val sykdomsvurderingForBrevNyBehandling = sykdomsvurderingForBrevForrigeBehandling.copy(behandlingId = tilBehandling)
            lagre(tilBehandling, sykdomsvurderingForBrevNyBehandling,)
        }
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