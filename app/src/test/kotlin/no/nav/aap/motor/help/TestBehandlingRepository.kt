package no.nav.aap.motor.help

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import no.nav.aap.verdityper.sakogbehandling.SakId
import no.nav.aap.verdityper.sakogbehandling.Status
import no.nav.aap.verdityper.sakogbehandling.TypeBehandling
import java.util.*

class TestBehandlingRepository(private val connection: DBConnection) {

    fun opprettBehandling(sakId: SakId, typeBehandling: TypeBehandling): BehandlingId {

        val query = """
            INSERT INTO BEHANDLING (sak_id, referanse, status, type)
                 VALUES (?, ?, ?, ?)
            """.trimIndent()
        val behandlingId = connection.executeReturnKey(query) {
            setParams {
                setLong(1, sakId.toLong())
                setUUID(2, UUID.randomUUID())
                setEnumName(3, Status.OPPRETTET)
                setString(4, typeBehandling.identifikator())
            }
        }

        return BehandlingId(behandlingId)
    }
}
