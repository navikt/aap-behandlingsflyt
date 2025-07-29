package no.nav.aap.behandlingsflyt.forretningsflyt.gjenopptak

import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.sakogbehandling.SakOgBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.repository.Repository
import no.nav.aap.komponenter.repository.RepositoryFactory

interface GjenopptakRepository : Repository {
    fun finnBehandlingerForGjennopptak(): List<SakOgBehandling>
}

class GjenopptakRepositoryImpl(private val connection: DBConnection) : GjenopptakRepository {
    override fun finnBehandlingerForGjennopptak(): List<SakOgBehandling> {
        val query = """
            SELECT b.id, b.sak_id 
            FROM BEHANDLING b
             JOIN AVKLARINGSBEHOV a ON a.behandling_id = b.id
             JOIN (
                SELECT DISTINCT ON (AVKLARINGSBEHOV_ID) *
                FROM AVKLARINGSBEHOV_ENDRING
                ORDER BY AVKLARINGSBEHOV_ID, OPPRETTET_TID DESC
             ) ae ON ae.AVKLARINGSBEHOV_ID = a.id
            WHERE b.STATUS in ('${Status.UTREDES.name}', '${Status.OPPRETTET.name}')
            AND ae.status = ?
            AND ae.frist <= CURRENT_DATE
        """.trimIndent()

        return connection.queryList(query) {
            setParams {
                setEnumName(1, no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status.OPPRETTET)
            }
            setRowMapper { row ->
                SakOgBehandling(sakId = SakId(row.getLong("sak_id")), behandlingId = BehandlingId(row.getLong("id")))
            }
        }
    }

    companion object : RepositoryFactory<GjenopptakRepository> {
        override fun konstruer(connection: DBConnection) = GjenopptakRepositoryImpl(connection)
    }
}