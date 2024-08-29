package no.nav.aap.behandlingsflyt.sakogbehandling.lås

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Saksnummer
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import no.nav.aap.verdityper.sakogbehandling.SakId
import java.util.*

class TaSkriveLåsRepository(private val connection: DBConnection) {

    fun lås(sakId: SakId, behandlingId: BehandlingId): Skrivelås {
        val sakSkrivelås = låsSak(sakId)
        val behandling = låsBehandling(behandlingId)
        return Skrivelås(sakSkrivelås, behandling)
    }

    fun låsBehandling(behandlingId: BehandlingId): BehandlingSkrivelås {
        val query = """SELECT versjon FROM BEHANDLING WHERE ID = ? FOR UPDATE"""

        return connection.queryFirst(query) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper {
                BehandlingSkrivelås(behandlingId, it.getLong("versjon"))
            }
        }
    }

    fun lås(behandlingUUid: UUID): Skrivelås {
        val query = """SELECT id, sak_id, versjon FROM BEHANDLING WHERE referanse = ? FOR UPDATE"""

        return connection.queryFirst(query) {
            setParams {
                setUUID(1, behandlingUUid)
            }
            setRowMapper {
                Skrivelås(
                    låsSak(SakId(it.getLong("sak_id"))),
                    BehandlingSkrivelås(
                        BehandlingId(it.getLong("id")),
                        it.getLong("versjon")
                    )
                )
            }
        }
    }

    fun låsSak(saksnummer: Saksnummer): SakSkrivelås {
        val query = """SELECT id,versjon FROM SAK WHERE saksnummer = ? FOR UPDATE"""

        return connection.queryFirst(query) {
            setParams {
                setString(1, saksnummer.toString())
            }
            setRowMapper {
                SakSkrivelås(SakId(it.getLong("id")), it.getLong("versjon"))
            }
        }
    }

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

    fun verifiserSkrivelås(skrivelås: Skrivelås) {
        verifiserSkrivelås(skrivelås.behandlingSkrivelås)
        verifiserSkrivelås(skrivelås.sakSkrivelås)
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
                setLong(2, skrivelås.id.toLong())
                setLong(3, skrivelås.versjon)
            }
            setResultValidator {
                require(it == 1)
            }
        }
    }
}
