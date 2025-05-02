
package no.nav.aap.behandlingsflyt.repository.log

import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.log.ContextRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.lookup.repository.Factory

class ContextRepositoryImpl(private val connection: DBConnection) : ContextRepository {

    companion object : Factory<ContextRepositoryImpl> {
        override fun konstruer(connection: DBConnection): ContextRepositoryImpl {
            return ContextRepositoryImpl(connection)
        }
    }

    override fun hentDataFor(behandlingReferanse: BehandlingReferanse): Map<String, String>? {
        val query = """
            SELECT s.id as sakId, s.saksnummer, b.id as behandlingId, b.type, b.referanse
            FROM behandling b 
            INNER JOIN sak s ON b.sak_id = s.id
            WHERE b.referanse = ?
        """.trimIndent()

        return connection.queryFirstOrNull(query) {
            setParams {
                setUUID(1, behandlingReferanse.referanse)
            }
            setRowMapper { rad ->
                mapRad(rad)
            }
        }
    }

    override fun hentDataFor(saksnummer: Saksnummer): Map<String, String>? {
        val query = """
            SELECT s.id as sakId, s.saksnummer, b.id as behandlingId, b.type, b.referanse
            FROM behandling b 
            INNER JOIN sak s ON b.sak_id = s.id
            WHERE s.saksnummer = ?
        """.trimIndent()

        return connection.queryFirstOrNull(query) {
            setParams {
                setString(1, saksnummer.toString())
            }
            setRowMapper { rad ->
                mapRad(rad)
            }
        }
    }

    private fun mapRad(row: Row): Map<String, String> = mapOf(
        "sakId" to row.getString("sakId"),
        "behandlingId" to row.getString("behandlingId"),
        "saksnummer" to row.getString("saksnummer"),
        "behandlingType" to row.getString("type"),
        "behandlingReferanse" to row.getUUID("referanse").toString(),
    )

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        // Denne trengs ikke implementeres
    }
}