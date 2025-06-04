package no.nav.aap.behandlingsflyt.repository.behandling

import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingMedVedtak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.StegTilstand
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Årsak
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.lookup.repository.Factory
import java.time.LocalDateTime

class BehandlingRepositoryImpl(private val connection: DBConnection) : BehandlingRepository {

    companion object : Factory<BehandlingRepositoryImpl> {
        override fun konstruer(connection: DBConnection): BehandlingRepositoryImpl {
            return BehandlingRepositoryImpl(connection)
        }
    }

    private val sakRepository = SakRepositoryImpl(connection)

    override fun opprettBehandling(
        sakId: SakId,
        årsaker: List<Årsak>,
        typeBehandling: TypeBehandling,
        forrigeBehandlingId: BehandlingId?
    ): Behandling {

        val query = """
            INSERT INTO BEHANDLING (sak_id, referanse, status, type, forrige_id)
                 VALUES (?, ?, ?, ?, ?)
            """.trimIndent()
        val behandlingsreferanse = BehandlingReferanse()

        val behandlingId = connection.executeReturnKey(query) {
            setParams {
                setLong(1, sakId.toLong())
                setUUID(2, behandlingsreferanse.referanse)
                setEnumName(3, Status.OPPRETTET)
                setString(4, typeBehandling.identifikator())
                setLong(5, forrigeBehandlingId?.toLong())
            }
        }

        val årsakQuery = """
            INSERT INTO AARSAK_TIL_BEHANDLING (behandling_id, aarsak, periode)
            VALUES (?, ?, ?::daterange)
        """.trimIndent()

        connection.executeBatch(årsakQuery, årsaker) {
            setParams {
                setLong(1, behandlingId)
                setEnumName(2, it.type)
                setPeriode(3, it.periode)
            }
        }

        return hent(BehandlingId(behandlingId))
    }

    override fun finnSisteBehandlingFor(sakId: SakId, behandlingstypeFilter: List<TypeBehandling>): Behandling? {
        val query = """
            SELECT * FROM BEHANDLING WHERE sak_id = ? AND type = ANY(?::text[]) ORDER BY opprettet_tid DESC LIMIT 1
            """.trimIndent()

        return connection.queryFirstOrNull(query) {
            setParams {
                setLong(1, sakId.toLong())
                setArray(2, behandlingstypeFilter.map { it.identifikator() })
            }
            setRowMapper(::mapBehandling)
        }
    }

    private fun mapBehandling(row: Row): Behandling {
        val behandlingId = BehandlingId(row.getLong("id"))
        return Behandling(
            id = behandlingId,
            referanse = BehandlingReferanse(row.getUUID("referanse")),
            sakId = SakId(row.getLong("sak_id")),
            typeBehandling = TypeBehandling.Companion.from(row.getString("type")),
            status = row.getEnum("status"),
            stegTilstand = hentAktivtSteg(behandlingId),
            versjon = row.getLong("versjon"),
            årsaker = hentÅrsaker(behandlingId),
            opprettetTidspunkt = row.getLocalDateTime("opprettet_tid"),
            forrigeBehandlingId = row.getLongOrNull("forrige_id")?.let { BehandlingId(it) }
        )
    }

    private fun mapBehandlingMedVedtak(row: Row): BehandlingMedVedtak {
        val behandlingId = BehandlingId(row.getLong("id"))
        return BehandlingMedVedtak(
            id = behandlingId,
            referanse = BehandlingReferanse(row.getUUID("referanse")),
            typeBehandling = TypeBehandling.Companion.from(row.getString("type")),
            status = row.getEnum("status"),
            opprettetTidspunkt = row.getLocalDateTime("opprettet_tid"),
            vedtakstidspunkt = row.getLocalDateTime("vedtakstidspunkt"),
            virkningstidspunkt = row.getLocalDateOrNull("virkningstidspunkt"),
        )
    }

    private fun hentÅrsaker(behandlingId: BehandlingId): List<Årsak> {
        val query = """
            SELECT * FROM AARSAK_TIL_BEHANDLING WHERE behandling_id = ? ORDER BY opprettet_tid DESC
        """.trimIndent()
        return connection.queryList(query) {
            setParams {
                setLong(1, behandlingId.id)
            }
            setRowMapper {
                Årsak(it.getEnum("aarsak"), it.getPeriodeOrNull("periode"))
            }
        }
    }

    override fun oppdaterBehandlingStatus(
        behandlingId: BehandlingId,
        status: Status
    ) {
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

    override fun leggTilNyttAktivtSteg(behandlingId: BehandlingId, tilstand: StegTilstand) {
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

    override fun flyttForrigeBehandlingId(
        behandlingId: BehandlingId,
        nyForrigeBehandlingId: BehandlingId
    ) {
        connection.execute("""
            update behandling
            set forrige_id = ?
            where id = ?
        """) {
            setParams {
                setLong(1, nyForrigeBehandlingId.id)
                setLong(2, behandlingId.id)
            }
        }
    }

    fun hentAktivtSteg(behandlingId: BehandlingId): StegTilstand? {
        val query = """
            SELECT * FROM STEG_HISTORIKK WHERE behandling_id = ? AND AKTIV = true
        """.trimIndent()

        return connection.queryFirstOrNull(query) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper { row ->
                StegTilstand(
                    tidspunkt = row.getLocalDateTime("OPPRETTET_TID"),
                    stegType = row.getEnum("steg"),
                    stegStatus = row.getEnum("status"),
                    aktiv = row.getBoolean("aktiv"),
                )
            }
        }
    }

    override fun hentStegHistorikk(behandlingId: BehandlingId): List<StegTilstand> {
        val query = """
            SELECT * FROM STEG_HISTORIKK WHERE behandling_id = ? ORDER BY opprettet_tid
        """.trimIndent()

        return connection.queryList(query) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper { row ->
                StegTilstand(
                    tidspunkt = row.getLocalDateTime("OPPRETTET_TID"),
                    stegType = row.getEnum("steg"),
                    stegStatus = row.getEnum("status"),
                    aktiv = row.getBoolean("aktiv"),
                )
            }
        }
    }

    override fun hentAlleFor(sakId: SakId, behandlingstypeFilter: List<TypeBehandling>): List<Behandling> {
        val query = """
            SELECT * FROM BEHANDLING WHERE sak_id = ?
             AND type = ANY(?::text[])
             ORDER BY opprettet_tid DESC
            """.trimIndent()

        return connection.queryList(query) {
            setParams {
                setLong(1, sakId.toLong())
                setArray(2, behandlingstypeFilter.map { it.identifikator() })
            }
            setRowMapper {
                mapBehandling(it)
            }
        }
    }

    override fun hentAlleMedVedtakFor(sakId: SakId, behandlingstypeFilter: List<TypeBehandling>): List<BehandlingMedVedtak> {
        val query = """
            SELECT * FROM BEHANDLING 
            INNER JOIN VEDTAK ON BEHANDLING.ID = VEDTAK.BEHANDLING_ID
            WHERE sak_id = ?
             AND type = ANY(?::text[])
             ORDER BY opprettet_tid DESC
            """.trimIndent()

        return connection.queryList(query) {
            setParams {
                setLong(1, sakId.toLong())
                setArray(2, behandlingstypeFilter.map { it.identifikator() })
            }
            setRowMapper {
                mapBehandlingMedVedtak(it)
            }
        }
    }

    override fun hent(behandlingId: BehandlingId): Behandling {
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

    override fun hentBehandlingType(behandlingId: BehandlingId): TypeBehandling {
        val query = """
            SELECT type FROM BEHANDLING WHERE id = ?
            """.trimIndent()

        return connection.queryFirst(query) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper { row ->
                TypeBehandling.Companion.from(row.getString("type"))
            }
        }
    }

    override fun hent(referanse: BehandlingReferanse): Behandling {
        val query = """
            SELECT * FROM BEHANDLING WHERE referanse = ?
            """.trimIndent()

        return connection.queryFirst(query) {
            setParams {
                setUUID(1, referanse.referanse)
            }
            setRowMapper {
                mapBehandling(it)
            }
        }
    }

    override fun oppdaterÅrsaker(behandling: Behandling, årsaker: List<Årsak>) {
        val årsakQuery = """
            INSERT INTO AARSAK_TIL_BEHANDLING (behandling_id, aarsak, periode)
            VALUES (?, ?, ?::daterange)
        """.trimIndent()

        connection.executeBatch(årsakQuery, årsaker.filter { !behandling.årsaker().contains(it) }) {
            setParams {
                setLong(1, behandling.id.toLong())
                setEnumName(2, it.type)
                setPeriode(3, it.periode)
            }
        }
    }

    override fun finnSøker(referanse: BehandlingReferanse): Person {
        val query = """
            SELECT SAK_ID FROM BEHANDLING WHERE referanse = ?

        """.trimIndent()
        return connection.queryFirst(query) {
            setParams {
                setUUID(1, referanse.referanse)
            }
            setRowMapper { row ->
                sakRepository.finnSøker(SakId(row.getLong("SAK_ID")))
            }
        }
    }

    override fun markerSavepoint() {
        connection.markerSavepoint()
    }

    override fun slett(behandlingId: BehandlingId) {
        // Ved sletting av behandling beholdes innholdet i alle relevante tabeller her. Det er ikke personopplysninger,
        // og er kritisk til at flyten skal kjøre.
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        // Trengs ikke implementeres
    }
}