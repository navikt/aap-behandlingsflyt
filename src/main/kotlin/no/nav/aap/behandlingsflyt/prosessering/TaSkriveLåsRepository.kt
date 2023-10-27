package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.dbstuff.DbConnection

class TaSkriveLåsRepository(private val connection: DbConnection) {

    fun låsSak(sakId: Long): Skrivelås {
        val query = """SELECT versjon FROM SAK WHERE ID = ? FOR UPDATE"""

        return connection.prepareFirstQueryStatement(query) {
            setParams {
                setLong(1, sakId)
            }
            setRowMapper {
                Skrivelås(sakId, it.getLong("versjon"))
            }
        }
    }

    fun verifiserSkrivelås(skrivelås: Skrivelås) {
        val query = """UPDATE sak SET versjon = ? WHERE ID = ? and versjon = ?"""

        return connection.prepareExecuteStatement(query) {
            setParams {
                setLong(1, skrivelås.sakVersjon + 1)
                setLong(2, skrivelås.sakId)
                setLong(3, skrivelås.sakVersjon)
            }
            setResultValidator {
                require(it == 1)
            }
        }
    }
}