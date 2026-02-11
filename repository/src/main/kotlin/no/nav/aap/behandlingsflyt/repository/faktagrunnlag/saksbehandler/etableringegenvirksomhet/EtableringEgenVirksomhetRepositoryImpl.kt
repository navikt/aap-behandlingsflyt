package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.etableringegenvirksomhet

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EtableringEgenVirksomhetGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EtableringEgenVirksomhetRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EtableringEgenVirksomhetVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.lookup.repository.Factory
import org.slf4j.LoggerFactory
import java.time.Instant

class EtableringEgenVirksomhetRepositoryImpl(private val connection: DBConnection) :
    EtableringEgenVirksomhetRepository {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object : Factory<EtableringEgenVirksomhetRepositoryImpl> {
        override fun konstruer(connection: DBConnection): EtableringEgenVirksomhetRepositoryImpl {
            return EtableringEgenVirksomhetRepositoryImpl(connection)
        }
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): EtableringEgenVirksomhetGrunnlag? {
        val query = """
            SELECT * FROM ETABLERING_EGEN_VIRKSOMHET_GRUNNLAG WHERE behandling_id = ? and aktiv = true
        """.trimIndent()

        return connection.queryFirstOrNull(query) {
            setParams {
                setLong(1, behandlingId.id)
            }
            setRowMapper { row ->
                EtableringEgenVirksomhetGrunnlag(
                    row.getLong("vurderinger_id").let(::mapVurdering)
                )
            }
        }
    }

    override fun lagre(
        behandlingId: BehandlingId,
        etableringEgenvirksomhetVurderinger: List<EtableringEgenVirksomhetVurdering>
    ) {
        deaktiverGrunnlag(behandlingId)

        val query = """
            INSERT INTO ETABLERING_EGEN_VIRKSOMHET_VURDERINGER (OPPRETTET_TID)
            VALUES (?)
        """.trimIndent()
        val vurderingerId = connection.executeReturnKey(query) {
            setParams { setInstant(1, Instant.now()) }
        }

        connection.executeBatch(
            """
            INSERT INTO ETABLERING_EGEN_VIRKSOMHET_VURDERING (BEGRUNNELSE, FORELIGGER_FAGLIG_VURDERING, VIRKSOMHET_ER_NY, BRUKER_EIER_VIRKSOMHET, KAN_BLI_SELVFORSORGET, VIRKSOMHET_NAVN, ORG_NR, EGEN_VIRKSOMHET_UTVIKLING_PERIODER_ID, EGEN_VIRKSOMHET_OPPSTART_PERIODER_ID, VURDERINGER_ID, VURDERT_I_BEHANDLING, VURDERT_AV, GJELDER_FRA, GJELDER_TIL, OPPRETTET_TID)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? ,? ,?, ?)
        """.trimIndent(), etableringEgenvirksomhetVurderinger
        ) {
            setParams {
                setString(1, it.begrunnelse)
                setBoolean(2, it.foreliggerFagligVurdering)
                setBoolean(3, it.virksomhetErNy)
                setBoolean(4, it.brukerEierVirksomheten)
                setBoolean(5, it.kanFøreTilSelvforsørget)
                setString(6, it.virksomhetNavn)
                setString(7, it.orgNr)
                setLong(8, lagreUtviklingsperiode(it.utviklingsPerioder))
                setLong(9, lagreOppstartsperiode(it.oppstartsPerioder))
                setLong(10, vurderingerId)
                setLong(11, it.vurdertIBehandling.id)
                setString(12, it.vurdertAv.ident)
                setLocalDate(13, it.vurderingenGjelderFra)
                setLocalDate(14, it.vurderingenGjelderTil)
                setInstant(15, it.opprettetTid)
            }
        }

        connection.execute(
            """
                INSERT INTO ETABLERING_EGEN_VIRKSOMHET_GRUNNLAG (BEHANDLING_ID, VURDERINGER_ID, OPPRETTET_TID) VALUES (?, ?, ?)
            """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.id)
                setLong(2, vurderingerId)
                setInstant(3, Instant.now())
            }
        }
    }

    private fun lagreUtviklingsperiode(perioder: List<Periode>): Long {
        val utviklingPerioderQuery = """
            INSERT INTO EGEN_VIRKSOMHET_UTVIKLING_PERIODER (OPPRETTET_TID)
            VALUES (?)
        """.trimIndent()
        val utviklingPerioderId = connection.executeReturnKey(utviklingPerioderQuery) {
            setParams { setInstant(1, Instant.now()) }
        }

        connection.executeBatch(
            """
                INSERT INTO EGEN_VIRKSOMHET_UTVIKLING_PERIODE (PERIODER_ID, PERIODE, OPPRETTET_TID) VALUES (?, ?::daterange, ?)
            """.trimIndent(), perioder
        ) {
            setParams {
                setLong(1, utviklingPerioderId)
                setPeriode(2, it)
                setInstant(3, Instant.now())
            }
        }

        return utviklingPerioderId
    }

    private fun mapVurdering(vurderingerId: Long): List<EtableringEgenVirksomhetVurdering> {
        val query = """
            SELECT * FROM ETABLERING_EGEN_VIRKSOMHET_VURDERING WHERE vurderinger_id = ?
        """.trimIndent()

        return connection.queryList(query) {
            setParams {
                setLong(1, vurderingerId)
            }
            setRowMapper { row ->
                EtableringEgenVirksomhetVurdering(
                    begrunnelse = row.getString("BEGRUNNELSE"),
                    virksomhetNavn = row.getString("VIRKSOMHET_NAVN"),
                    orgNr = row.getStringOrNull("ORG_NR"),
                    foreliggerFagligVurdering = row.getBoolean("FORELIGGER_FAGLIG_VURDERING"),
                    virksomhetErNy = row.getBoolean("VIRKSOMHET_ER_NY"),
                    brukerEierVirksomheten = row.getBoolean("BRUKER_EIER_VIRKSOMHET"),
                    kanFøreTilSelvforsørget = row.getBoolean("KAN_BLI_SELVFORSORGET"),
                    utviklingsPerioder = row.getLongOrNull("EGEN_VIRKSOMHET_UTVIKLING_PERIODER_ID")
                        .let(::hentVirksomhetUtviklingsperioder),
                    oppstartsPerioder = row.getLongOrNull("EGEN_VIRKSOMHET_OPPSTART_PERIODER_ID")
                        .let(::hentVirksomhetOppstartsperioder),
                    vurdertAv = Bruker(row.getString("VURDERT_AV")),
                    opprettetTid = row.getInstant("OPPRETTET_TID"),
                    vurdertIBehandling = row.getLong("VURDERT_I_BEHANDLING").let(::BehandlingId),
                    vurderingenGjelderFra = row.getLocalDate("GJELDER_FRA"),
                    vurderingenGjelderTil = row.getLocalDateOrNull("GJELDER_TIL")
                )
            }
        }
    }

    private fun hentVirksomhetUtviklingsperioder(utviklingsperioderId: Long?): List<Periode> {
        if (utviklingsperioderId == null) return emptyList()
        val query = """
            SELECT * FROM EGEN_VIRKSOMHET_UTVIKLING_PERIODE WHERE PERIODER_ID = ?
        """.trimIndent()
        return connection.queryList(query) {
            setParams { setLong(1, utviklingsperioderId) }
            setRowMapper { row -> row.getPeriode("PERIODE") }
        }
    }

    private fun hentVirksomhetOppstartsperioder(oppstartsperioderId: Long?): List<Periode> {
        if (oppstartsperioderId == null) return emptyList()
        val query = """
            SELECT * FROM EGEN_VIRKSOMHET_OPPSTART_PERIODE WHERE PERIODER_ID = ?
        """.trimIndent()
        return connection.queryList(query) {
            setParams { setLong(1, oppstartsperioderId) }
            setRowMapper { row -> row.getPeriode("PERIODE") }
        }
    }

    private fun lagreOppstartsperiode(perioder: List<Periode>): Long {
        val oppstartPerioderQuery = """
            INSERT INTO EGEN_VIRKSOMHET_OPPSTART_PERIODER (OPPRETTET_TID)
            VALUES (?)
        """.trimIndent()
        val oppstartPerioderId = connection.executeReturnKey(oppstartPerioderQuery) {
            setParams { setInstant(1, Instant.now()) }
        }

        connection.executeBatch(
            """
                INSERT INTO EGEN_VIRKSOMHET_OPPSTART_PERIODE (PERIODER_ID, PERIODE, OPPRETTET_TID) VALUES (?, ?::daterange, ?)
            """.trimIndent(), perioder
        ) {
            setParams {
                setLong(1, oppstartPerioderId)
                setPeriode(2, it)
                setInstant(3, Instant.now())
            }
        }

        return oppstartPerioderId
    }

    private fun deaktiverGrunnlag(behandlingId: BehandlingId) {
        connection.execute("UPDATE ETABLERING_EGEN_VIRKSOMHET_GRUNNLAG set aktiv = false WHERE behandling_id = ? and aktiv = true") {
            setParams {
                setLong(1, behandlingId.id)
            }
        }
    }

    override fun slett(behandlingId: BehandlingId) {
        val vurderingerGrunnlagIds = getVurderingerIds(behandlingId)
        val vurderingerIds = vurderingerGrunnlagIds.map { it.vurderingerId }
        val utviklingIds = vurderingerGrunnlagIds.mapNotNull { it.utviklingsperioderId }
        val oppstartIds = vurderingerGrunnlagIds.mapNotNull { it.oppstartsperioderId }

        val deletedRows = connection.executeReturnUpdated(
            """
                delete from ETABLERING_EGEN_VIRKSOMHET_GRUNNLAG where behandling_id = ?; 
                delete from ETABLERING_EGEN_VIRKSOMHET_VURDERING where vurderinger_id = ANY(?::bigint[]);
                delete from ETABLERING_EGEN_VIRKSOMHET_VURDERINGER where id = ANY(?::bigint[]);
                delete from EGEN_VIRKSOMHET_UTVIKLING_PERIODE where PERIODER_ID = ANY(?::bigint[]);
                delete from EGEN_VIRKSOMHET_OPPSTART_PERIODE where PERIODER_ID = ANY(?::bigint[]);
                delete from EGEN_VIRKSOMHET_UTVIKLING_PERIODER where id = ANY(?::bigint[]);
                delete from EGEN_VIRKSOMHET_OPPSTART_PERIODER where id = ANY(?::bigint[]);
            """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.id)
                setLongArray(2, vurderingerIds)
                setLongArray(3, vurderingerIds)
                setLongArray(4, utviklingIds)
                setLongArray(5, utviklingIds)
                setLongArray(6, oppstartIds)
                setLongArray(7, oppstartIds)
            }
        }
        log.info("Slettet $deletedRows rader fra EtableringEgenVirksomhetGrunnlag")
    }

    override fun kopier(
        fraBehandling: BehandlingId,
        tilBehandling: BehandlingId
    ) {
        hentHvisEksisterer(fraBehandling) ?: return

        val query = """
            INSERT INTO ETABLERING_EGEN_VIRKSOMHET_GRUNNLAG (behandling_id, vurderinger_id, opprettet_tid)
            SELECT ?, vurderinger_id, ? from ETABLERING_EGEN_VIRKSOMHET_GRUNNLAG where behandling_id = ? and aktiv
        """.trimIndent()
        connection.execute(query) {
            setParams {
                setLong(1, tilBehandling.id)
                setInstant(2, Instant.now())
                setLong(3, fraBehandling.id)
            }
        }
    }

    private fun getVurderingerIds(
        behandlingId: BehandlingId
    ): List<InternalEtableringEgenVirksomhetGrunnlag> =
        connection.queryList(
            """
            SELECT
                g.vurderinger_id,
                v.egen_virksomhet_utvikling_perioder_id,
                v.egen_virksomhet_oppstart_perioder_id
            FROM ETABLERING_EGEN_VIRKSOMHET_GRUNNLAG g
            JOIN ETABLERING_EGEN_VIRKSOMHET_VURDERING v
                ON v.vurderinger_id = g.vurderinger_id
            WHERE g.behandling_id = ?
              AND g.aktiv = TRUE
        """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.id)
            }
            setRowMapper { row ->
                InternalEtableringEgenVirksomhetGrunnlag(
                    vurderingerId = row.getLong("vurderinger_id"),
                    utviklingsperioderId = row.getLongOrNull("egen_virksomhet_utvikling_perioder_id"),
                    oppstartsperioderId = row.getLongOrNull("egen_virksomhet_oppstart_perioder_id"),
                )
            }
        }

    private data class InternalEtableringEgenVirksomhetGrunnlag(
        val vurderingerId: Long,
        val utviklingsperioderId: Long?,
        val oppstartsperioderId: Long?,
    )
}