package no.nav.aap.behandlingsflyt.flyt.internal

import no.nav.aap.behandlingsflyt.dbstuff.DBConnection
import no.nav.aap.behandlingsflyt.sak.Status

class FlytOperasjonRepository(private val connection: DBConnection) {

    fun oppdaterSakStatus(sakId: Long, status: Status) {
        val query = """UPDATE sak SET status = ? WHERE ID = ?"""

        return connection.prepareExecuteStatement(query) {
            setParams {
                setString(1, status.name)
                setLong(2, sakId)
            }
            setResultValidator {
                require(it == 1)
            }
        }
    }
}
