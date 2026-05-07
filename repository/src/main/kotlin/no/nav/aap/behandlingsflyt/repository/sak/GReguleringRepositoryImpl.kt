package no.nav.aap.behandlingsflyt.repository.sak

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.GReguleringRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.Factory

class GReguleringRepositoryImpl(private val connection: DBConnection) : GReguleringRepository {

    companion object : Factory<GReguleringRepositoryImpl> {
        override fun konstruer(connection: DBConnection): GReguleringRepositoryImpl {
            return GReguleringRepositoryImpl(connection)
        }
    }

    override fun harGReguleringForÅr(sakId: SakId, år: Int): Boolean {
        return connection.queryFirstOrNull(
            """SELECT 1 FROM g_regulering_historikk WHERE sak_id = ? AND regulerings_aar = ?"""
        ) {
            setParams {
                setLong(1, sakId.toLong())
                setInt(2, år)
            }
            setRowMapper { true }
        } ?: false
    }

    override fun registrerGRegulering(sakId: SakId, år: Int) {
        connection.execute(
            """INSERT INTO g_regulering_historikk (sak_id, regulerings_aar) VALUES (?, ?)
               ON CONFLICT (sak_id, regulerings_aar) DO NOTHING"""
        ) {
            setParams {
                setLong(1, sakId.toLong())
                setInt(2, år)
            }
        }
    }

    override fun slett(behandlingId: BehandlingId) {
        // Ingenting å slette ved trekking av søknad
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        // G-reguleringsdata kopieres ikke mellom behandlinger
    }
}
