package no.nav.aap.behandlingsflyt.repository.behandling

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingMedVedtak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.StegTilstand
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.lookup.repository.Factory
import no.nav.aap.meldekort.kontrakt.Periode
import java.time.LocalDateTime

class BehandlingRepositoryImpl(private val connection: DBConnection) : BehandlingRepository {

    companion object : Factory<BehandlingRepositoryImpl> {
        override fun konstruer(connection: DBConnection): BehandlingRepositoryImpl {
            return BehandlingRepositoryImpl(connection)
        }
    }

    override fun opprettBehandling(
        sakId: SakId,
        typeBehandling: TypeBehandling,
        forrigeBehandlingId: BehandlingId?,
        vurderingsbehovOgÅrsak: VurderingsbehovOgÅrsak,
    ): Behandling {

        val query = """
            INSERT INTO BEHANDLING (sak_id, referanse, status, type, forrige_id, aarsak_til_opprettelse)
                 VALUES (?, ?, ?, ?, ?, ?)
            """.trimIndent()
        val behandlingsreferanse = BehandlingReferanse()

        val behandlingId = connection.executeReturnKey(query) {
            setParams {
                setLong(1, sakId.toLong())
                setUUID(2, behandlingsreferanse.referanse)
                setEnumName(3, Status.OPPRETTET)
                setString(4, typeBehandling.identifikator())
                setLong(5, forrigeBehandlingId?.toLong())
                setEnumName(6, vurderingsbehovOgÅrsak.årsak)
            }
        }

        val årsakQuery = """
            INSERT INTO behandling_aarsak(behandling_id, aarsak, begrunnelse, opprettet_tid)
            VALUES (?, ?, ?, ?)
        """.trimIndent()

        val behandlingÅrsakId = connection.executeReturnKey(årsakQuery, {
            setParams {
                setLong(1, behandlingId)
                setEnumName(2, vurderingsbehovOgÅrsak.årsak)
                setString(3, vurderingsbehovOgÅrsak.beskrivelse)
                setLocalDateTime(4, vurderingsbehovOgÅrsak.opprettet)
            }
        })

        val vurderingsbehovQuery = """
            INSERT INTO vurderingsbehov (behandling_id, aarsak, periode, behandling_aarsak_id)
            VALUES (?, ?, ?::daterange, ?)
        """.trimIndent()

        connection.executeBatch(vurderingsbehovQuery, vurderingsbehovOgÅrsak.vurderingsbehov) {
            setParams {
                setLong(1, behandlingId)
                setEnumName(2, it.type)
                setPeriode(3, it.periode)
                setLong(4, behandlingÅrsakId)
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

    override fun finnSaksnummer(referanse: BehandlingReferanse): Saksnummer {
        return connection.queryFirst(
            """
            SELECT saksnummer FROM sak 
            INNER JOIN behandling ON behandling.sak_id = sak.id
            WHERE behandling.referanse = (?)
            """.trimIndent()
        ) {
            setParams {
                setUUID(1, referanse.referanse)
            }
            setRowMapper { Saksnummer(it.getString("saksnummer")) }
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
            vurderingsbehov = hentVurderingsbehov(behandlingId),
            opprettetTidspunkt = row.getLocalDateTime("opprettet_tid"),
            årsakTilOpprettelse = row.getEnumOrNull("aarsak_til_opprettelse"),
            forrigeBehandlingId = row.getLongOrNull("forrige_id")?.let { BehandlingId(it) }
        )
    }

    private fun mapBehandlingMedVedtakForPerson(row: Row): BehandlingMedVedtak {
        val behandlingId = BehandlingId(row.getLong("id"))
        return BehandlingMedVedtak(
            saksnummer = Saksnummer(row.getString("saksnummer")),
            id = behandlingId,
            referanse = BehandlingReferanse(row.getUUID("referanse")),
            typeBehandling = TypeBehandling.Companion.from(row.getString("type")),
            status = row.getEnum("status"),
            opprettetTidspunkt = row.getLocalDateTime("opprettet_tid"),
            vedtakstidspunkt = row.getLocalDateTime("vedtakstidspunkt"),
            virkningstidspunkt = row.getLocalDateOrNull("virkningstidspunkt"),
            vurderingsbehov = hentVurderingsbehov(behandlingId).map { it.type }.toSet(),
            årsakTilOpprettelse = row.getEnumOrNull("aarsak_til_opprettelse"),
        )
    }

    private fun hentVurderingsbehov(behandlingId: BehandlingId): List<VurderingsbehovMedPeriode> {
        val query = """
            SELECT * FROM vurderingsbehov WHERE behandling_id = ? ORDER BY opprettet_tid DESC
        """.trimIndent()
        return connection.queryList(query) {
            setParams {
                setLong(1, behandlingId.id)
            }
            setRowMapper {
                VurderingsbehovMedPeriode(it.getEnum("aarsak"), it.getPeriodeOrNull("periode"))
            }
        }
    }

    override fun hentVurderingsbehovOgÅrsaker(behandlingId: BehandlingId): List<VurderingsbehovOgÅrsak> {
        data class VurderingsbehovOgÅrsakInternal(
            val id: Long,
            val vurderingsbehovType: Vurderingsbehov,
            val vurderingsbehovPeriode: no.nav.aap.komponenter.type.Periode?,
            val årsak: ÅrsakTilOpprettelse,
            val opprettet: LocalDateTime,
            val beskrivelse: String?
        )

        val query = """
            SELECT ba.id as aarsak_id, ba.aarsak, ba.begrunnelse, ba.opprettet_tid,
                   vb.aarsak as vurderingsbehov, vb.periode
            FROM behandling_aarsak ba
            INNER JOIN vurderingsbehov vb ON vb.behandling_aarsak_id = ba.id
            WHERE vb.behandling_id = ?
            ORDER BY ba.opprettet_tid DESC, vb.opprettet_tid DESC
        """.trimIndent()

        val vurderingsbehovOgÅrsakInternal = connection.queryList(query) {
            setParams {
                setLong(1, behandlingId.id)
            }
            setRowMapper { row ->
                VurderingsbehovOgÅrsakInternal(
                    id = row.getLong("aarsak_id"),
                    årsak = row.getEnum("aarsak"),
                    beskrivelse = row.getStringOrNull("begrunnelse"),
                    opprettet = row.getLocalDateTime("opprettet_tid"),
                    vurderingsbehovType = row.getEnum("vurderingsbehov"),
                    vurderingsbehovPeriode = row.getPeriodeOrNull("periode")
                )
            }
        }

        return vurderingsbehovOgÅrsakInternal
            .groupBy { it.id }
            .map { (_, vurderingsbehovOgÅrsak) ->
                VurderingsbehovOgÅrsak(
                    årsak = vurderingsbehovOgÅrsak.first().årsak,
                    beskrivelse = vurderingsbehovOgÅrsak.first().beskrivelse,
                    opprettet = vurderingsbehovOgÅrsak.first().opprettet,
                    vurderingsbehov = vurderingsbehovOgÅrsak.map { VurderingsbehovMedPeriode(it.vurderingsbehovType, it.vurderingsbehovPeriode) }
                )
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
        connection.execute(
            """
            update behandling
            set forrige_id = ?
            where id = ?
        """
        ) {
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

    override fun hentAlleMedVedtakFor(
        person: Person,
        behandlingstypeFilter: List<TypeBehandling>
    ): List<BehandlingMedVedtak> {
        val query = """
            SELECT
                S.SAKSNUMMER,
                B.ID,
                B.REFERANSE,
                B.TYPE,
                B.STATUS,
                B.OPPRETTET_TID,
                B.AARSAK_TIL_OPPRETTELSE,
                V.VEDTAKSTIDSPUNKT,
                V.VIRKNINGSTIDSPUNKT
            FROM
                SAK S
                INNER JOIN BEHANDLING B ON B.SAK_ID = S.ID
                INNER JOIN VEDTAK V ON V.BEHANDLING_ID = B.ID
            WHERE
                S.PERSON_ID = ?
                AND TYPE = ANY(?::TEXT[])
            ORDER BY
                OPPRETTET_TID DESC

        """.trimIndent()

        return connection.queryList(query) {
            setParams {
                setLong(1, person.id.id)
                setArray(2, behandlingstypeFilter.map { it.identifikator() })
            }
            setRowMapper {
                mapBehandlingMedVedtakForPerson(it)
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

    override fun oppdaterVurderingsbehov(behandling: Behandling, vurderingsbehovOgÅrsak: VurderingsbehovOgÅrsak) {
        val nyeVurderingsbehov = vurderingsbehovOgÅrsak.vurderingsbehov.filter { !behandling.vurderingsbehov().contains(it) }

        val årsakQuery = """
            INSERT INTO behandling_aarsak(behandling_id, aarsak, begrunnelse, opprettet_tid)
            VALUES (?, ?, ?, ?)
        """.trimIndent()

        val behandlingÅrsakId = connection.executeReturnKey(årsakQuery, {
            setParams {
                setLong(1, behandling.id.toLong())
                setEnumName(2, vurderingsbehovOgÅrsak.årsak)
                setString(3, vurderingsbehovOgÅrsak.beskrivelse)
                setLocalDateTime(4, vurderingsbehovOgÅrsak.opprettet)
            }
        })

        val vurderingsbehovQuery = """
            INSERT INTO vurderingsbehov (behandling_id, aarsak, periode, behandling_aarsak_id)
            VALUES (?, ?, ?::daterange, ?)
        """.trimIndent()

        connection.executeBatch(vurderingsbehovQuery, nyeVurderingsbehov) {
            setParams {
                setLong(1, behandling.id.toLong())
                setEnumName(2, it.type)
                setPeriode(3, it.periode)
                setLong(4, behandlingÅrsakId)
            }
        }
    }

    override fun hentSakId(referanse: BehandlingReferanse): SakId {
        val query = """
            SELECT SAK_ID FROM BEHANDLING WHERE referanse = ?
        """.trimIndent()
        return connection.queryFirst(query) {
            setParams {
                setUUID(1, referanse.referanse)
            }
            setRowMapper { row ->
                SakId(row.getLong("SAK_ID"))
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