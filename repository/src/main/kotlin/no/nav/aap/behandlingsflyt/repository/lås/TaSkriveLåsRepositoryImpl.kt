package no.nav.aap.behandlingsflyt.repository.lås

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.lås.BehandlingSkrivelås
import no.nav.aap.behandlingsflyt.sakogbehandling.lås.SakSkrivelås
import no.nav.aap.behandlingsflyt.sakogbehandling.lås.Skrivelås
import no.nav.aap.behandlingsflyt.sakogbehandling.lås.TaSkriveLåsRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.Factory
import org.slf4j.LoggerFactory
import java.util.*

class TaSkriveLåsRepositoryImpl(private val connection: DBConnection): TaSkriveLåsRepository {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object : Factory<TaSkriveLåsRepositoryImpl> {
        override fun konstruer(connection: DBConnection): TaSkriveLåsRepositoryImpl {
            return TaSkriveLåsRepositoryImpl(connection)
        }
    }

    override fun lås(sakId: SakId, behandlingId: BehandlingId): Skrivelås {
        val sakSkrivelås = låsSak(sakId)
        val behandling = låsBehandling(behandlingId)
        return Skrivelås(sakSkrivelås, behandling)
    }

    override fun withLås(
        sakId: SakId,
        behandlingId: BehandlingId,
        block: (Skrivelås) -> Unit
    ) {
        val lås = lås(sakId, behandlingId)
        block(lås)
        verifiserSkrivelås(lås)
    }

    override fun låsBehandling(behandlingId: BehandlingId): BehandlingSkrivelås {
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

    override fun withLåstBehandling(
        behandlingId: BehandlingId,
        block: (BehandlingSkrivelås) -> Unit
    ) {
        val skrivelås = låsBehandling(behandlingId)
        block(skrivelås)
        verifiserSkrivelås(skrivelås)
    }

    override fun lås(behandlingUUid: UUID): Skrivelås {
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

    override fun låsSak(saksnummer: Saksnummer): SakSkrivelås {
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

    override fun låsSak(sakId: SakId): SakSkrivelås {
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

    override fun verifiserSkrivelås(skrivelås: Skrivelås) {
        verifiserSkrivelås(skrivelås.behandlingSkrivelås)
        verifiserSkrivelås(skrivelås.sakSkrivelås)
    }

    override fun verifiserSkrivelås(skrivelås: SakSkrivelås) {
        log.info("Oppdaterer fra versjon ${skrivelås.versjon} på sak med id ${skrivelås.id.id}")
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

    override fun verifiserSkrivelås(skrivelås: BehandlingSkrivelås) {
        log.info("Oppdaterer fra versjon ${skrivelås.versjon} på behandling med id ${skrivelås.id.id}")
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

    override fun slett(behandlingId: BehandlingId) {
        // Sletting av behandling gjøres i BehandlingRepository
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        // Trengs ikke implementeres
    }
}