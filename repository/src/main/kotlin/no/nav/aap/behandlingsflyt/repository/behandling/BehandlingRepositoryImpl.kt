package no.nav.aap.behandlingsflyt.repository.behandling

import no.nav.aap.behandlingsflyt.behandling.vedtak.VedtakId
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.SakOgBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingMedVedtak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.StegTilstand
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovMedPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.VurderingsbehovOgÅrsak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.Vurderingsbehov
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Query
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.lookup.repository.Factory
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
            INSERT INTO behandling_aarsak(behandling_id, aarsak, begrunnelse, opprettet_tid, opprettet_av)
            VALUES (?, ?, ?, ?, ?)
        """.trimIndent()

        val behandlingÅrsakId = connection.executeReturnKey(årsakQuery) {
            setParams {
                setLong(1, behandlingId)
                setEnumName(2, vurderingsbehovOgÅrsak.årsak)
                setString(3, vurderingsbehovOgÅrsak.beskrivelse)
                setLocalDateTime(4, vurderingsbehovOgÅrsak.opprettet)
                setBruker(5, vurderingsbehovOgÅrsak.opprettetAv)
            }
        }

        val vurderingsbehovQuery = """
            INSERT INTO vurderingsbehov (behandling_id, aarsak, behandling_aarsak_id, opprettet_tid, oppdatert_tid)
            VALUES (?, ?, ?, ?, ?)
        """.trimIndent()

        val opprettetTid = LocalDateTime.now()
        connection.executeBatch(vurderingsbehovQuery, vurderingsbehovOgÅrsak.vurderingsbehov) {
            setParams {
                setLong(1, behandlingId)
                setEnumName(2, it.type)
                setLong(3, behandlingÅrsakId)
                setLocalDateTime(4, opprettetTid)
                setLocalDateTime(5, opprettetTid)
            }
        }

        return hent(BehandlingId(behandlingId))
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

    override fun finnAlleGjeldendeVedtatteBehandlinger(): List<SakOgBehandling> {
        return connection.queryList(
            """
                SELECT * FROM gjeldende_vedtatte_behandlinger
            """.trimIndent()
        ) {
            setRowMapper {
                SakOgBehandling(
                    sakId = SakId(it.getLong("sak_id")),
                    behandlingId = BehandlingId(it.getLong("behandling_id"))
                )
            }
        }
    }

    override fun finnGjeldendeVedtattBehandlingForSak(sakId: SakId): SakOgBehandling? {
        return connection.queryFirstOrNull(
            """
                SELECT * FROM gjeldende_vedtatte_behandlinger where sak_id = ?
            """.trimIndent()
        ) {
            setParams {
                setLong(1, sakId.toLong())
            }
            setRowMapper {
                SakOgBehandling(
                    sakId = SakId(it.getLong("sak_id")),
                    behandlingId = BehandlingId(it.getLong("behandling_id"))
                )
            }
        }
    }

    private data class VurderingsbehovJson(val aarsak: String, val tid: String)

    private fun mapVurderingsbehov(row: Row): List<VurderingsbehovMedPeriode> {
        val json = row.getStringOrNull("vb_json") ?: return emptyList()
        return DefaultJsonMapper.fromJson<List<VurderingsbehovJson>>(json).map {
            VurderingsbehovMedPeriode(
                type = Vurderingsbehov.valueOf(it.aarsak),
                oppdatertTid = LocalDateTime.parse(it.tid)
            )
        }.distinct()
    }

    private fun mapBehandling(row: Row): Behandling {
        val behandlingId = BehandlingId(row.getLong("id"))
        return Behandling(
            id = behandlingId,
            referanse = BehandlingReferanse(row.getUUID("referanse")),
            sakId = SakId(row.getLong("sak_id")),
            typeBehandling = TypeBehandling.from(row.getString("type")),
            status = row.getEnum("status"),
            stegTilstand = row.getEnumOrNull<StegType>("sh_steg")?.let { stegType ->
                StegTilstand(
                    tidspunkt = row.getLocalDateTime("sh_opprettet_tid"),
                    stegType = stegType,
                    stegStatus = row.getEnum("sh_status"),
                )
            },
            versjon = row.getLong("versjon"),
            vurderingsbehov = mapVurderingsbehov(row),
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
            forrigeBehandlingId = row.getLongOrNull("forrige_id")?.let { BehandlingId(it) },
            referanse = BehandlingReferanse(row.getUUID("referanse")),
            typeBehandling = TypeBehandling.from(row.getString("type")),
            status = row.getEnum("status"),
            opprettetTidspunkt = row.getLocalDateTime("opprettet_tid"),
            vedtakstidspunkt = row.getLocalDateTime("vedtakstidspunkt"),
            virkningstidspunkt = row.getLocalDateOrNull("virkningstidspunkt"),
            vurderingsbehov = mapVurderingsbehov(row).map { it.type }.toSet(),
            årsakTilOpprettelse = row.getEnumOrNull("aarsak_til_opprettelse"),
            vedtakId = VedtakId(row.getLong("vedtak_id")),
        )
    }

    /**
     * Merk at denne ikke vil returnere vurderingsbehov og årsaker for behandlinger før tabellen `behandling_aarsak` ble introdusert.
     * Det er besluttet å ikke migrere data for denne perioden da det mangler data for å gjøre dette korrekt samt at det er snakk
     * om få behandlinger hvor dette er aktuelt.
     */
    override fun hentVurderingsbehovOgÅrsaker(behandlingId: BehandlingId): List<VurderingsbehovOgÅrsak> {
        data class VurderingsbehovOgÅrsakInternal(
            val id: Long,
            val vurderingsbehovType: Vurderingsbehov,
            val vurderingsbehovOppdatertTid: LocalDateTime,
            val årsak: ÅrsakTilOpprettelse,
            val opprettet: LocalDateTime,
            val opprettetAv: Bruker?,
            val beskrivelse: String?
        )

        val query = """
            SELECT ba.id as aarsak_id, ba.aarsak, ba.begrunnelse, ba.opprettet_tid, ba.opprettet_av,
                   vb.aarsak as vurderingsbehov, vb.opprettet_tid as vb_opprettet_Tid, vb.oppdatert_tid as vb_oppdatert_tid
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
                    opprettetAv = row.getBrukerOrNull("opprettet_av"),
                    vurderingsbehovType = row.getEnum("vurderingsbehov"),
                    vurderingsbehovOppdatertTid = row.getLocalDateTimeOrNull("vb_oppdatert_tid")
                        ?: row.getLocalDateTime("vb_opprettet_Tid")
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
                    opprettetAv = vurderingsbehovOgÅrsak.first().opprettetAv,
                    vurderingsbehov = vurderingsbehovOgÅrsak.map {
                        VurderingsbehovMedPeriode(
                            it.vurderingsbehovType,
                            it.vurderingsbehovOppdatertTid
                        )
                    }
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

    private fun setStegtilstand(behandlingId: BehandlingId): Query<StegTilstand>.() -> Unit {
        return {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper { row ->
                StegTilstand(
                    tidspunkt = row.getLocalDateTime("OPPRETTET_TID"),
                    stegType = row.getEnum("steg"),
                    stegStatus = row.getEnum("status"),
                )
            }
        }
    }

    override fun hentStegHistorikk(behandlingId: BehandlingId): List<StegTilstand> {
        val query = """
            SELECT * FROM STEG_HISTORIKK WHERE behandling_id = ? ORDER BY opprettet_tid, id
        """.trimIndent()

        return connection.queryList(query, setStegtilstand(behandlingId))
    }

    override fun hentAlleFor(sakId: SakId, behandlingstypeFilter: List<TypeBehandling>): List<Behandling> {
        val query = """
            SELECT b.*, sh.steg AS sh_steg, sh.status AS sh_status, sh.opprettet_tid AS sh_opprettet_tid,
                   vb_agg.vb_json
            FROM BEHANDLING b
            LEFT JOIN STEG_HISTORIKK sh ON sh.behandling_id = b.id AND sh.aktiv
            LEFT JOIN LATERAL (
                SELECT json_agg(json_build_object('aarsak', aarsak, 'tid', COALESCE(oppdatert_tid, opprettet_tid)) ORDER BY opprettet_tid DESC) AS vb_json
                FROM vurderingsbehov
                WHERE behandling_id = b.id
            ) vb_agg ON true
            WHERE b.sak_id = ?
             AND b.type = ANY(?::text[])
             ORDER BY b.opprettet_tid DESC
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

    override fun hentAlleIkkeAvbrutteYtelsesbehandlinger(sakId: SakId): List<Behandling> {
        val query = """
            with avbrutt_behandling as (
                select avbryt_revurdering_grunnlag.behandling_id as behandling_id
                from avbryt_revurdering_grunnlag
                join avbryt_revurdering_vurdering on avbryt_revurdering_grunnlag.vurdering_id = avbryt_revurdering_vurdering.id
                where avbryt_revurdering_grunnlag.aktiv
            )
            select b.*, sh.steg AS sh_steg, sh.status AS sh_status, sh.opprettet_tid AS sh_opprettet_tid,
                   vb_agg.vb_json
            from behandling b
            left join avbrutt_behandling on avbrutt_behandling.behandling_id = b.id
            left join steg_historikk sh on sh.behandling_id = b.id and sh.aktiv
            left join lateral (
                select json_agg(json_build_object('aarsak', aarsak, 'tid', COALESCE(oppdatert_tid, opprettet_tid)) ORDER BY opprettet_tid DESC) AS vb_json
                from vurderingsbehov
                where behandling_id = b.id
            ) vb_agg on true
            where b.sak_id = ?
            and b.type in (${TypeBehandling.ytelseBehandlingstyper().joinToString { "'${it.identifikator()}'"} })
            and avbrutt_behandling.behandling_id is null
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

    override fun hentAlleMedVedtakFor(
        personId: PersonId,
        behandlingstypeFilter: List<TypeBehandling>
    ): List<BehandlingMedVedtak> {
        val query = """
            SELECT
                S.SAKSNUMMER,
                B.ID,
                B.FORRIGE_ID,
                B.REFERANSE,
                B.TYPE,
                B.STATUS,
                B.OPPRETTET_TID,
                B.AARSAK_TIL_OPPRETTELSE,
                V.VEDTAKSTIDSPUNKT,
                V.VIRKNINGSTIDSPUNKT,
                V.ID AS VEDTAK_ID,
                vb_agg.vb_json
            FROM
                SAK S
                INNER JOIN BEHANDLING B ON B.SAK_ID = S.ID
                INNER JOIN VEDTAK V ON V.BEHANDLING_ID = B.ID
                LEFT JOIN LATERAL (
                    SELECT json_agg(json_build_object('aarsak', aarsak, 'tid', COALESCE(oppdatert_tid, opprettet_tid)) ORDER BY opprettet_tid DESC) AS vb_json
                    FROM vurderingsbehov
                    WHERE behandling_id = B.ID
                ) vb_agg ON true
            WHERE
                S.PERSON_ID = ?
                AND TYPE = ANY(?::TEXT[])
            ORDER BY
                OPPRETTET_TID DESC
        """.trimIndent()

        return connection.queryList(query) {
            setParams {
                setLong(1, personId.id)
                setArray(2, behandlingstypeFilter.map { it.identifikator() })
            }
            setRowMapper {
                mapBehandlingMedVedtakForPerson(it)
            }
        }
    }

    override fun hent(behandlingId: BehandlingId): Behandling {
        val query = """
            SELECT b.*, sh.steg AS sh_steg, sh.status AS sh_status, sh.opprettet_tid AS sh_opprettet_tid,
                   vb_agg.vb_json
            FROM BEHANDLING b
            LEFT JOIN STEG_HISTORIKK sh ON sh.behandling_id = b.id AND sh.aktiv
            LEFT JOIN LATERAL (
                SELECT json_agg(json_build_object('aarsak', aarsak, 'tid', COALESCE(oppdatert_tid, opprettet_tid)) ORDER BY opprettet_tid DESC) AS vb_json
                FROM vurderingsbehov
                WHERE behandling_id = b.id
            ) vb_agg ON true
            WHERE b.id = ?
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

    override fun hent(referanse: BehandlingReferanse): Behandling {
        val query = """
            SELECT b.*, sh.steg AS sh_steg, sh.status AS sh_status, sh.opprettet_tid AS sh_opprettet_tid,
                   vb_agg.vb_json
            FROM BEHANDLING b
            LEFT JOIN STEG_HISTORIKK sh ON sh.behandling_id = b.id AND sh.aktiv
            LEFT JOIN LATERAL (
                SELECT json_agg(json_build_object('aarsak', aarsak, 'tid', COALESCE(oppdatert_tid, opprettet_tid)) ORDER BY opprettet_tid DESC) AS vb_json
                FROM vurderingsbehov
                WHERE behandling_id = b.id
            ) vb_agg ON true
            WHERE b.referanse = ?
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

    override fun finnFørstegangsbehandling(sakId: SakId): Behandling {
        val query = """
            SELECT b.*, sh.steg AS sh_steg, sh.status AS sh_status, sh.opprettet_tid AS sh_opprettet_tid,
                   vb_agg.vb_json
            FROM BEHANDLING b
            LEFT JOIN STEG_HISTORIKK sh ON sh.behandling_id = b.id AND sh.aktiv
            LEFT JOIN LATERAL (
                SELECT json_agg(json_build_object('aarsak', aarsak, 'tid', COALESCE(oppdatert_tid, opprettet_tid)) ORDER BY opprettet_tid DESC) AS vb_json
                FROM vurderingsbehov
                WHERE behandling_id = b.id
            ) vb_agg ON true
            WHERE b.sak_id = ? AND b.type = ?
            """.trimIndent()

        return connection.queryFirst(query) {
            setParams {
                setLong(1, sakId.toLong())
                setString(2, TypeBehandling.Førstegangsbehandling.identifikator())
            }
            setRowMapper {
                mapBehandling(it)
            }
        }
    }

    override fun oppdaterVurderingsbehovOgÅrsak(
        behandling: Behandling,
        vurderingsbehovOgÅrsak: VurderingsbehovOgÅrsak
    ) {
        val årsakQuery = """
            INSERT INTO behandling_aarsak(behandling_id, aarsak, begrunnelse, opprettet_tid, opprettet_av)
            VALUES (?, ?, ?, ?, ?)
        """.trimIndent()

        val behandlingÅrsakId = connection.executeReturnKey(årsakQuery) {
            setParams {
                setLong(1, behandling.id.toLong())
                setEnumName(2, vurderingsbehovOgÅrsak.årsak)
                setString(3, vurderingsbehovOgÅrsak.beskrivelse)
                setLocalDateTime(4, vurderingsbehovOgÅrsak.opprettet)
                setBruker(5, vurderingsbehovOgÅrsak.opprettetAv)
            }
        }

        val vurderingsbehovQuery = """
            INSERT INTO vurderingsbehov (behandling_id, aarsak, behandling_aarsak_id, opprettet_tid, oppdatert_tid)
            VALUES (?, ?, ?, ?, ?)
        """.trimIndent()

        connection.executeBatch(vurderingsbehovQuery, vurderingsbehovOgÅrsak.vurderingsbehov) {
            setParams {
                setLong(1, behandling.id.toLong())
                setEnumName(2, it.type)
                setLong(3, behandlingÅrsakId)
                val nå = LocalDateTime.now()
                setLocalDateTime(4, nå)
                setLocalDateTime(5, nå)
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

    fun hentKandidatForStansOpphørBackfill(behandlingId: Long): Behandling? {
        return connection.queryFirstOrNull("""
            select b.*, sh.steg AS sh_steg, sh.status AS sh_status, sh.opprettet_tid AS sh_opprettet_tid,
                   vb_agg.vb_json
            from behandling b
            left join steg_historikk sh on sh.behandling_id = b.id and sh.aktiv
            left join lateral (
                select json_agg(json_build_object('aarsak', aarsak, 'tid', COALESCE(oppdatert_tid, opprettet_tid)) ORDER BY opprettet_tid DESC) AS vb_json
                from vurderingsbehov
                where behandling_id = b.id
            ) vb_agg on true
            where
            b.id = ?
            and b.type IN ('ae0034', 'ae0028')
            and (b.type <> 'ae0034' or b.status <> 'OPPRETTET')
            ${if (Miljø.erDev()) "and b.opprettet_tid >= '2025-04-01'::date" else ""}
            """.trimIndent()) {
            setParams {
                setLong(1, behandlingId)
            }
            setRowMapper {
                mapBehandling(it)
            }
        }
    }
}
