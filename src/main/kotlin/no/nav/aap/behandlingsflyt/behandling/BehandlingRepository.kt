package no.nav.aap.behandlingsflyt.behandling

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.dbstuff.DBConnection
import no.nav.aap.behandlingsflyt.dbstuff.Row
import no.nav.aap.behandlingsflyt.faktagrunnlag.GrunnlagKopierer
import no.nav.aap.behandlingsflyt.flyt.behandlingstyper.Førstegangsbehandling
import no.nav.aap.behandlingsflyt.flyt.behandlingstyper.Revurdering
import no.nav.aap.behandlingsflyt.flyt.steg.StegStatus
import no.nav.aap.behandlingsflyt.flyt.steg.StegType
import no.nav.aap.behandlingsflyt.flyt.steg.Tilstand
import java.util.*

class BehandlingRepository(private val connection: DBConnection) {

    private val avklaringsbehovRepository = AvklaringsbehovRepository(connection)

    fun opprettBehandling(sakId: Long, årsaker: List<Årsak>): Behandling {
        val sisteBehandlingFor = finnSisteBehandlingFor(sakId)
        val erSisteBehandlingAvsluttet = sisteBehandlingFor?.status()?.erAvsluttet() ?: true

        if (!erSisteBehandlingAvsluttet) {
            throw IllegalStateException("Siste behandling er ikke avsluttet")
        }

        val behandlingType = utledBehandlingType(sisteBehandlingFor != null)
        val referanse = UUID.randomUUID()

        val query = """
            INSERT INTO BEHANDLING (sak_id, referanse, status, type)
                 VALUES (?, ?, ?, ?)
            """.trimIndent()
        val behandlingId = connection.executeReturnKeys(query) {
            setParams {
                setLong(1, sakId)
                setUUID(2, referanse)
                setString(3, Status.OPPRETTET.name)
                setString(4, behandlingType.identifikator())
            }
        }.first()

        val behandling = Behandling(id = behandlingId, sakId = sakId, type = behandlingType, årsaker = årsaker)
        if (sisteBehandlingFor != null) {
            GrunnlagKopierer.overfør(sisteBehandlingFor, behandling)
        }

        return behandling
    }

    fun finnSisteBehandlingFor(sakId: Long): Behandling? {
        val query = """
            SELECT * FROM BEHANDLING WHERE sak_id = ? ORDER BY opprettet_tid DESC LIMIT 1
            """.trimIndent()

        return connection.queryFirstOrNull(query) {
            setParams {
                setLong(1, sakId)
            }
            setRowMapper {
                mapBehandling(it)
            }
        }
    }

    private fun mapBehandling(it: Row): Behandling {
        val behandlingId = it.getLong("id")
        return Behandling(
            id = behandlingId,
            referanse = it.getUUID("referanse"),
            sakId = it.getLong("sak_id"),
            type = utledType(it.getString("type")),
            status = Status.valueOf(it.getString("status")),
            avklaringsbehovene = avklaringsbehovRepository.hent(behandlingId),
            stegHistorikk = hentStegHistorikk(behandlingId)
        )
    }

    private fun hentStegHistorikk(behandlingId: Long): List<StegTilstand> {
        val query = """
            SELECT * FROM STEG_HISTORIKK WHERE behandling_id = ? and aktiv = true
        """.trimIndent()

        return connection.queryList(query) {
            setParams {
                setLong(1, behandlingId)
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

    fun hentAlleFor(sakId: Long): List<Behandling> {
        val query = """
            SELECT * FROM BEHANDLING WHERE sak_id = ? ORDER BY opprettet_tid DESC
            """.trimIndent()

        return connection.queryList(query) {
            setParams {
                setLong(1, sakId)
            }
            setRowMapper {
                mapBehandling(it)
            }
        }
    }

    fun hent(behandlingId: Long): Behandling {
        val query = """
            SELECT * FROM BEHANDLING WHERE id = ?
            """.trimIndent()

        return connection.queryFirst(query) {
            setParams {
                setLong(1, behandlingId)
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
