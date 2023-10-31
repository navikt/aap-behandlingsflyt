package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.dbstuff.DBConnection
import no.nav.aap.behandlingsflyt.sak.SakId

class TaSkriveLåsRepository(private val connection: DBConnection) {

    fun låsSak(sakId: SakId): SakSkrivelås {
        val query = """SELECT versjon FROM SAK WHERE ID = ? FOR UPDATE"""

        return connection.queryFirst(query) {
            setParams {
                setLong(1, sakId.toLong())
            }
            setRowMapper {
                SakSkrivelås(sakId, it.getLong("versjon"))
            }
        }
    }

    fun lås(sakId: SakId, behandlingId: Long): Skrivelås {
        val behandling = låsBehandling(behandlingId)
        val sakSkrivelås = låsSak(sakId)
        return Skrivelås(sakSkrivelås, behandling)
    }

    fun låsBehandling(behandlingId: Long): BehandlingSkrivelås {
        val query = """SELECT versjon FROM BEHANDLING WHERE ID = ? FOR UPDATE"""

        return connection.queryFirst(query) {
            setParams {
                setLong(1, behandlingId)
            }
            setRowMapper {
                BehandlingSkrivelås(behandlingId, it.getLong("versjon"))
            }
        }
    }

    fun verifiserSkrivelås(skrivelås: SakSkrivelås) {
        val query = """UPDATE sak SET versjon = ? WHERE ID = ? and versjon = ?"""

        return connection.execute(query) {
            setParams {
                setLong(1, skrivelås.versjon + 1)
                setLong(2, skrivelås.id.toLong())
                setLong(3, skrivelås.versjon)
            }
            setResultValidator {
                require(it == 1)
            }
        }
    }

    fun verifiserSkrivelås(skrivelås: BehandlingSkrivelås) {
        val query = """UPDATE behandling SET versjon = ? WHERE ID = ? and versjon = ?"""

        return connection.execute(query) {
            setParams {
                setLong(1, skrivelås.versjon + 1)
                setLong(2, skrivelås.id)
                setLong(3, skrivelås.versjon)
            }
            setResultValidator {
                require(it == 1)
            }
        }
    }

    fun verifiserSkrivelås(skrivelås: Skrivelås) {
        verifiserSkrivelås(skrivelås.behandlingSkrivelås)
        verifiserSkrivelås(skrivelås.sakSkrivelås)
    }
}
