package no.nav.aap.behandlingsflyt.behandling

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.dbconnect.Row
import no.nav.aap.behandlingsflyt.faktagrunnlag.GrunnlagKopierer
import no.nav.aap.behandlingsflyt.flyt.behandlingstyper.Førstegangsbehandling
import no.nav.aap.behandlingsflyt.flyt.behandlingstyper.Revurdering
import no.nav.aap.behandlingsflyt.flyt.steg.StegStatus
import no.nav.aap.behandlingsflyt.flyt.steg.StegType
import no.nav.aap.behandlingsflyt.flyt.steg.Tilstand
import no.nav.aap.behandlingsflyt.sak.SakId
import java.time.LocalDateTime
import java.util.*

class BehandlingRepository(private val connection: DBConnection) {

    fun opprettBehandling(sakId: SakId, årsaker: List<Årsak>): Behandling {
        val sisteBehandlingFor = finnSisteBehandlingFor(sakId)
        val erSisteBehandlingAvsluttet = sisteBehandlingFor?.status()?.erAvsluttet() ?: true

        if (!erSisteBehandlingAvsluttet) {
            throw IllegalStateException("Siste behandling er ikke avsluttet")
        }

        val behandlingType = utledBehandlingType(sisteBehandlingFor != null)
        val referanse = UUID.randomUUID() //TODO: Hva gjør vi her med refaranse?

        val query = """
            INSERT INTO BEHANDLING (sak_id, referanse, status, type)
                 VALUES (?, ?, ?, ?)
            """.trimIndent()
        val behandlingId = connection.executeReturnKey(query) {
            setParams {
                setLong(1, sakId.toLong())
                setUUID(2, referanse)
                setEnumName(3, Status.OPPRETTET)
                setString(4, behandlingType.identifikator())
            }
        }

        val behandling = Behandling(
            id = BehandlingId(behandlingId),
            sakId = sakId,
            type = behandlingType,
            årsaker = årsaker,
            versjon = 0
        )
        if (sisteBehandlingFor != null) {
            GrunnlagKopierer(connection).overfør(sisteBehandlingFor, behandling)
        }

        return behandling
    }

    fun finnSisteBehandlingFor(sakId: SakId): Behandling? {
        val query = """
            SELECT * FROM BEHANDLING WHERE sak_id = ? ORDER BY opprettet_tid DESC LIMIT 1
            """.trimIndent()

        return connection.queryFirstOrNull(query) {
            setParams {
                setLong(1, sakId.toLong())
            }
            setRowMapper(::mapBehandling)
        }
    }

    private fun mapBehandling(it: Row): Behandling {
        val behandlingId = BehandlingId(it.getLong("id"))
        return Behandling(
            id = behandlingId,
            referanse = it.getUUID("referanse"),
            sakId = SakId(it.getLong("sak_id")),
            type = utledType(it.getString("type")),
            status = Status.valueOf(it.getString("status")),
            stegHistorikk = hentStegHistorikk(behandlingId),
            versjon = it.getLong("versjon")
        )
    }

    fun oppdaterBehandlingStatus(behandlingId: BehandlingId, status: no.nav.aap.behandlingsflyt.behandling.Status) {
        val query = """UPDATE behandling SET status = ? WHERE ID = ?"""

        return connection.execute(query) {
            setParams {
                setEnumName(1, status)
                setLong(2, behandlingId.toLong())
            }
            setResultValidator {
                require(it == 1)
            }
        }
    }

    fun loggBesøktSteg(behandlingId: BehandlingId, tilstand: Tilstand) {
        val updateQuery = """
            UPDATE STEG_HISTORIKK set aktiv = false WHERE behandling_id = ? and aktiv = true
        """.trimIndent()

        connection.execute(updateQuery) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
        }

        val query = """
                INSERT INTO STEG_HISTORIKK (behandling_id, steg, status, aktiv, opprettet_tid) 
                VALUES (?, ?, ?, ?, ?)
                """.trimIndent()

        connection.execute(query) {
            setParams {
                setLong(1, behandlingId.toLong())
                setEnumName(2, tilstand.steg())
                setEnumName(3, tilstand.status())
                setBoolean(4, true)
                setLocalDateTime(5, LocalDateTime.now())
            }
        }
    }

    private fun hentStegHistorikk(behandlingId: BehandlingId): List<StegTilstand> {
        val query = """
            SELECT * FROM STEG_HISTORIKK WHERE behandling_id = ? and aktiv = true
        """.trimIndent()

        return connection.queryList(query) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper {
                StegTilstand(
                    tidspunkt = it.getLocalDateTime("OPPRETTET_TID"),
                    tilstand = Tilstand(
                        type = StegType.valueOf(it.getString("steg")),
                        status = StegStatus.valueOf(it.getString("status"))
                    )
                )
            }
        }
    }

    private fun utledBehandlingType(present: Boolean): BehandlingType {
        if (present) {
            return Revurdering
        }
        return Førstegangsbehandling
    }

    fun hentAlleFor(sakId: SakId): List<Behandling> {
        val query = """
            SELECT * FROM BEHANDLING WHERE sak_id = ? ORDER BY opprettet_tid DESC
            """.trimIndent()

        return connection.queryList(query) {
            setParams {
                setLong(1, sakId.toLong())
            }
            setRowMapper {
                mapBehandling(it)
            }
        }
    }

    fun hent(behandlingId: BehandlingId): Behandling {
        val query = """
            SELECT * FROM BEHANDLING WHERE id = ?
            """.trimIndent()

        return connection.queryFirst(query) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper {
                mapBehandling(it)
            }
        }
    }

    fun hent(referanse: UUID): Behandling {
        val query = """
            SELECT * FROM BEHANDLING WHERE referanse = ?
            """.trimIndent()

        return connection.queryFirst(query) {
            setParams {
                setUUID(1, referanse)
            }
            setRowMapper {
                mapBehandling(it)
            }
        }
    }
}
