package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.sykdom

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.YrkesskadeSak
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Yrkesskadevurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.lookup.repository.Factory
import no.nav.aap.verdityper.dokument.JournalpostId
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime

class SykdomRepositoryImpl(private val connection: DBConnection) : SykdomRepository {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object : Factory<SykdomRepositoryImpl> {
        override fun konstruer(connection: DBConnection): SykdomRepositoryImpl {
            return SykdomRepositoryImpl(connection)
        }
    }

    private fun deaktiverGrunnlag(behandlingId: BehandlingId) {
        connection.execute("UPDATE SYKDOM_GRUNNLAG SET AKTIV = FALSE WHERE BEHANDLING_ID = ? AND AKTIV = TRUE") {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setResultValidator { require(it == 1) }
        }
    }

    override fun lagre(
        behandlingId: BehandlingId,
        sykdomsvurderinger: List<Sykdomsvurdering>,
    ) {
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)
        val nyttGrunnlag = SykdomGrunnlag(
            yrkesskadevurdering = eksisterendeGrunnlag?.yrkesskadevurdering,
            sykdomsvurderinger = sykdomsvurderinger
        )

        if (eksisterendeGrunnlag != nyttGrunnlag) {
            eksisterendeGrunnlag?.let {
                deaktiverGrunnlag(behandlingId)
            }

            lagre(behandlingId, nyttGrunnlag)
        }
    }

    override fun lagre(
        behandlingId: BehandlingId, yrkesskadevurdering: Yrkesskadevurdering?
    ) {
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)
        val nyttGrunnlag = SykdomGrunnlag(
            yrkesskadevurdering = yrkesskadevurdering,
            sykdomsvurderinger = eksisterendeGrunnlag?.sykdomsvurderinger.orEmpty(),
        )

        if (eksisterendeGrunnlag != nyttGrunnlag) {
            eksisterendeGrunnlag?.let {
                deaktiverGrunnlag(behandlingId)
            }

            lagre(behandlingId, nyttGrunnlag)
        }
    }

    override fun slett(behandlingId: BehandlingId) {
        val sykdomVurderingerIds = getSykdomVurderingerIds(behandlingId)
        val sykdomVurderingIds = getSykdomVurderingIds(sykdomVurderingerIds)
        val yrkesskadevurderingIds = getYrkesskadeVurderingIds(behandlingId)

        val deletedRows = connection.executeReturnUpdated(
            """
            delete from sykdom_grunnlag where behandling_id = ?; 
            delete from sykdom_vurdering_bidiagnoser where vurdering_id = ANY(?::bigint[]);
            delete from sykdom_vurdering_dokumenter where vurdering_id = ANY(?::bigint[]);
            delete from sykdom_vurdering where sykdom_vurderinger_id = ANY(?::bigint[]);
            delete from sykdom_vurderinger where id = ANY(?::bigint[]);
            delete from yrkesskade_relaterte_saker where vurdering_id = ANY(?::bigint[]);
            delete from yrkesskade_vurdering where id = ANY(?::bigint[]);
        """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.id)
                setLongArray(2, sykdomVurderingIds)
                setLongArray(3, sykdomVurderingIds)
                setLongArray(4, sykdomVurderingerIds)
                setLongArray(5, sykdomVurderingerIds)
                setLongArray(6, yrkesskadevurderingIds)
                setLongArray(7, yrkesskadevurderingIds)
            }
        }
        log.info("Slettet $deletedRows rader fra sykdom_grunnlag")
    }

    private fun getSykdomVurderingerIds(behandlingId: BehandlingId): List<Long> = connection.queryList(
        """
                    SELECT sykdom_vurderinger_id
                    FROM sykdom_grunnlag
                    WHERE behandling_id = ? AND sykdom_vurderinger_id is not null;
                 
                """.trimIndent()
    ) {
        setParams { setLong(1, behandlingId.id) }
        setRowMapper { row ->
            row.getLong("sykdom_vurderinger_id")
        }
    }

    private fun getSykdomVurderingIds(sykdomVurderingerIds: List<Long>): List<Long> = connection.queryList(
        """
                    SELECT id
                    FROM sykdom_vurdering
                    WHERE sykdom_vurderinger_id = ANY(?::bigint[]);
                 
                """.trimIndent()
    ) {
        setParams { setLongArray(1, sykdomVurderingerIds) }
        setRowMapper { row ->
            row.getLong("id")
        }
    }

    private fun getYrkesskadeVurderingIds(behandlingId: BehandlingId): List<Long> = connection.queryList(
        """
                    SELECT yrkesskade_id
                    FROM sykdom_grunnlag
                    WHERE behandling_id = ? AND yrkesskade_id is not null;
                 
                """.trimIndent()
    ) {
        setParams { setLong(1, behandlingId.id) }
        setRowMapper { row ->
            row.getLong("yrkesskade_id")
        }
    }


    private fun lagre(behandlingId: BehandlingId, nyttGrunnlag: SykdomGrunnlag) {
        val sykdomsvurderingerId = lagreSykdom(nyttGrunnlag.sykdomsvurderinger)
        val yrkesskadeId = lagreYrkesskade(nyttGrunnlag.yrkesskadevurdering)

        val query = """
            INSERT INTO SYKDOM_GRUNNLAG (BEHANDLING_ID, YRKESSKADE_ID, SYKDOM_VURDERINGER_ID) VALUES (?, ?, ?)
        """.trimIndent()

        connection.execute(query) {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, yrkesskadeId)
                setLong(3, sykdomsvurderingerId)
            }
        }
    }

    private fun lagreYrkesskade(vurdering: Yrkesskadevurdering?): Long? {
        if (vurdering == null) {
            return null
        }
        if (vurdering.id != null) {
            return vurdering.id
        }

        val query = """
            INSERT INTO YRKESSKADE_VURDERING 
            (BEGRUNNELSE, ARSAKSSAMMENHENG, ANDEL_AV_NEDSETTELSE, VURDERT_AV)
            VALUES
            (?, ?, ?, ?)
        """.trimIndent()

        val id = connection.executeReturnKey(query) {
            setParams {
                setString(1, vurdering.begrunnelse)
                setBoolean(2, vurdering.erÅrsakssammenheng)
                setInt(3, vurdering.andelAvNedsettelsen?.prosentverdi())
                setString(4, vurdering.vurdertAv)
            }
        }

        connection.executeBatch(
            """
            INSERT INTO YRKESSKADE_RELATERTE_SAKER (vurdering_id, referanse, manuell_yrkesskade_dato) VALUES (?, ?, ?)
        """.trimIndent(), vurdering.relevanteSaker
        ) {
            setParams { sak ->
                setLong(1, id)
                setString(2, sak.referanse)
                setLocalDate(3, sak.manuellYrkesskadeDato)
            }
        }

        return id
    }

    private fun lagreSykdom(vurderinger: List<Sykdomsvurdering>): Long {
        val sykdomsvurderingerId = connection.executeReturnKey("""INSERT INTO SYKDOM_VURDERINGER DEFAULT VALUES""")

        val query = """
            INSERT INTO SYKDOM_VURDERING (
                SYKDOM_VURDERINGER_ID,
                BEGRUNNELSE, VURDERINGEN_GJELDER_FRA,
                ER_ARBEIDSEVNE_NEDSATT, HAR_SYKDOM_SKADE_LYTE,
                ER_SYKDOM_SKADE_LYTE_VESETLING_DEL, ER_NEDSETTELSE_MER_ENN_HALVPARTEN,
                ER_NEDSETTELSE_MER_ENN_YRKESSKADE_GRENSE, ER_NEDSETTELSE_AV_EN_VISS_VARIGHET,
                YRKESSKADE_BEGRUNNELSE, KODEVERK,
                DIAGNOSE, OPPRETTET_TID, VURDERT_AV_IDENT, VURDERT_I_BEHANDLING, VURDERINGEN_GJELDER_TIL)
            VALUES
            (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()

        for (vurdering in vurderinger) {
            val id = connection.executeReturnKey(query) {
                setParams {
                    setLong(1, sykdomsvurderingerId)
                    setString(2, vurdering.begrunnelse)
                    setLocalDate(3, vurdering.vurderingenGjelderFra)
                    setBoolean(4, vurdering.erArbeidsevnenNedsatt)
                    setBoolean(5, vurdering.harSkadeSykdomEllerLyte)
                    setBoolean(6, vurdering.erSkadeSykdomEllerLyteVesentligdel)
                    setBoolean(7, vurdering.erNedsettelseIArbeidsevneMerEnnHalvparten)
                    setBoolean(8, vurdering.erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense)
                    setBoolean(9, vurdering.erNedsettelseIArbeidsevneAvEnVissVarighet)
                    setString(10, vurdering.yrkesskadeBegrunnelse)
                    setString(11, vurdering.kodeverk)
                    setString(12, vurdering.hoveddiagnose)
                    setInstant(13, vurdering.opprettet)
                    setString(14, vurdering.vurdertAv.ident)
                    setLong(15, vurdering.vurdertIBehandling?.id)
                    setLocalDate(16, vurdering.vurderingenGjelderTil)
                }
            }

            lagreSykdomDokumenter(id, vurdering.dokumenterBruktIVurdering)
            lagreBidiagnose(id, vurdering.bidiagnoser.orEmpty())
        }

        return sykdomsvurderingerId
    }

    private fun lagreBidiagnose(sykdomsId: Long, bidiagnoser: List<String>) {
        val query = """
            INSERT INTO SYKDOM_VURDERING_BIDIAGNOSER (VURDERING_ID, KODE) 
            VALUES (?, ?)
        """.trimIndent()

        connection.executeBatch(query, bidiagnoser) {
            setParams { kode ->
                setLong(1, sykdomsId)
                setString(2, kode)
            }
        }
    }


    private fun lagreSykdomDokumenter(sykdomsId: Long, journalpostIder: List<JournalpostId>) {
        val query = """
            INSERT INTO SYKDOM_VURDERING_DOKUMENTER (VURDERING_ID, JOURNALPOST) 
            VALUES (?, ?)
        """.trimIndent()

        connection.executeBatch(query, journalpostIder) {
            setParams {
                setLong(1, sykdomsId)
                setString(2, it.identifikator)
            }
        }
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        val eksisterendeGrunnlag = hentHvisEksisterer(fraBehandling)
        if (eksisterendeGrunnlag == null) {
            return
        }
        val query = """
            INSERT INTO SYKDOM_GRUNNLAG (BEHANDLING_ID, YRKESSKADE_ID, SYKDOM_VURDERINGER_ID)
            SELECT ?, YRKESSKADE_ID, SYKDOM_VURDERINGER_ID
            FROM SYKDOM_GRUNNLAG WHERE BEHANDLING_ID = ? AND AKTIV
        """.trimIndent()

        connection.execute(query) {
            setParams {
                setLong(1, tilBehandling.toLong())
                setLong(2, fraBehandling.toLong())
            }
        }
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): SykdomGrunnlag? {
        val query = """
            SELECT * FROM SYKDOM_GRUNNLAG WHERE BEHANDLING_ID = ? AND AKTIV = TRUE
        """.trimIndent()
        return connection.queryFirstOrNull(query) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper(::mapGrunnlag)
        }
    }

    private fun mapGrunnlag(row: Row): SykdomGrunnlag {
        return SykdomGrunnlag(
            mapYrkesskade(row.getLongOrNull("YRKESSKADE_ID")),
            mapSykdommer(row.getLongOrNull("SYKDOM_VURDERINGER_ID")),
        )
    }

    private fun mapSykdommer(sykdomVurderingerId: Long?): List<Sykdomsvurdering> {
        return connection.queryList(
            """
            SELECT id,
                   BEGRUNNELSE,
                   VURDERINGEN_GJELDER_FRA,
                   HAR_SYKDOM_SKADE_LYTE,
                   ER_SYKDOM_SKADE_LYTE_VESETLING_DEL,
                   ER_NEDSETTELSE_MER_ENN_HALVPARTEN,
                   ER_NEDSETTELSE_MER_ENN_YRKESSKADE_GRENSE,
                   ER_NEDSETTELSE_AV_EN_VISS_VARIGHET,
                   ER_ARBEIDSEVNE_NEDSATT,
                   YRKESSKADE_BEGRUNNELSE,
                   KODEVERK,
                   DIAGNOSE,
                   OPPRETTET_TID,
                   VURDERT_AV_IDENT,
                   VURDERT_I_BEHANDLING,
                   VURDERINGEN_GJELDER_TIL
            FROM SYKDOM_VURDERING
            WHERE SYKDOM_VURDERINGER_ID = ?
            """.trimIndent()
        ) {
            setParams {
                setLong(1, sykdomVurderingerId)
            }
            setRowMapper(::sykdomsvurderingRowmapper)
        }
    }


    private fun sykdomsvurderingRowmapper(row: Row): Sykdomsvurdering {
        val sykdomsvurderingId = row.getLong("id")
        return Sykdomsvurdering(
            id = sykdomsvurderingId,
            begrunnelse = row.getString("BEGRUNNELSE"),
            vurderingenGjelderFra = row.getLocalDateOrNull("VURDERINGEN_GJELDER_FRA"),
            dokumenterBruktIVurdering = hentSykdomsDokumenter(sykdomsvurderingId),
            harSkadeSykdomEllerLyte = row.getBoolean("HAR_SYKDOM_SKADE_LYTE"),
            erSkadeSykdomEllerLyteVesentligdel = row.getBooleanOrNull("ER_SYKDOM_SKADE_LYTE_VESETLING_DEL"),
            erNedsettelseIArbeidsevneMerEnnHalvparten = row.getBooleanOrNull("ER_NEDSETTELSE_MER_ENN_HALVPARTEN"),
            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = row.getBooleanOrNull("ER_NEDSETTELSE_MER_ENN_YRKESSKADE_GRENSE"),
            erNedsettelseIArbeidsevneAvEnVissVarighet = row.getBooleanOrNull("ER_NEDSETTELSE_AV_EN_VISS_VARIGHET"),
            erArbeidsevnenNedsatt = row.getBooleanOrNull("ER_ARBEIDSEVNE_NEDSATT"),
            yrkesskadeBegrunnelse = row.getStringOrNull("YRKESSKADE_BEGRUNNELSE"),
            kodeverk = row.getStringOrNull("KODEVERK"),
            hoveddiagnose = row.getStringOrNull("DIAGNOSE"),
            bidiagnoser = hentBidiagnoser(vurderingId = sykdomsvurderingId),
            opprettet = row.getInstant("OPPRETTET_TID"),
            vurdertAv = Bruker(row.getString("VURDERT_AV_IDENT")),
            vurdertIBehandling = row.getLongOrNull("VURDERT_I_BEHANDLING")?.let { BehandlingId(it) },
            vurderingenGjelderTil = row.getLocalDateOrNull("VURDERINGEN_GJELDER_TIL")
        )
    }

    private fun hentBidiagnoser(vurderingId: Long): List<String> {
        return connection.queryList("SELECT KODE FROM SYKDOM_VURDERING_BIDIAGNOSER WHERE VURDERING_ID = ?") {
            setParams {
                setLong(1, vurderingId)
            }
            setRowMapper { row ->
                row.getString("KODE")
            }
        }
    }

    private fun hentSykdomsDokumenter(yrkesskadeId: Long): List<JournalpostId> {
        return connection.queryList("SELECT JOURNALPOST FROM SYKDOM_VURDERING_DOKUMENTER WHERE VURDERING_ID = ?") {
            setParams {
                setLong(1, yrkesskadeId)
            }
            setRowMapper { row ->
                JournalpostId(row.getString("JOURNALPOST"))
            }
        }
    }

    private fun mapYrkesskade(yrkesskadeId: Long?): Yrkesskadevurdering? {
        if (yrkesskadeId == null) {
            return null
        }
        val query = """
            SELECT id, BEGRUNNELSE, ARSAKSSAMMENHENG, ANDEL_AV_NEDSETTELSE, VURDERT_AV, OPPRETTET_TID
            FROM YRKESSKADE_VURDERING
            WHERE ID = ?
        """.trimIndent()
        return connection.queryFirstOrNull(query) {
            setParams {
                setLong(1, yrkesskadeId)
            }
            setRowMapper { row ->
                val id = row.getLong("id")
                Yrkesskadevurdering(
                    id = id,
                    begrunnelse = row.getString("BEGRUNNELSE"),
                    erÅrsakssammenheng = row.getBoolean("ARSAKSSAMMENHENG"),
                    andelAvNedsettelsen = row.getIntOrNull("ANDEL_AV_NEDSETTELSE")?.let(::Prosent),
                    relevanteSaker = hentRelevanteSaker(id),
                    vurdertAv = row.getString("VURDERT_AV"),
                    // sjekk dette tidsssonemessig
                    vurdertTidspunkt = row.getLocalDateTime("OPPRETTET_TID"),
                )
            }
        }
    }

    private fun hentRelevanteSaker(vurderingId: Long): List<YrkesskadeSak> {
        val query = """
            SELECT * FROM YRKESSKADE_RELATERTE_SAKER WHERE vurdering_id = ?
        """.trimIndent()
        return connection.queryList(query) {
            setParams {
                setLong(1, vurderingId)
            }
            setRowMapper { row ->
                YrkesskadeSak(row.getString("REFERANSE"), row.getLocalDateOrNull("manuell_yrkesskade_dato"))
            }
        }
    }

    override fun hent(behandlingId: BehandlingId): SykdomGrunnlag {
        return requireNotNull(hentHvisEksisterer(behandlingId)) { "Fant ikke sykdomsgrunnlag for behandling med ID $behandlingId." }
    }

    override fun hentHistoriskeSykdomsvurderinger(sakId: SakId, behandlingId: BehandlingId): List<Sykdomsvurdering> {
        val query = """
            SELECT DISTINCT on(vurdering.opprettet_tid) vurdering.*
            FROM SYKDOM_GRUNNLAG grunnlag
            INNER JOIN SYKDOM_VURDERINGER vurderinger ON grunnlag.SYKDOM_VURDERINGER_ID = vurderinger.ID
            INNER JOIN SYKDOM_VURDERING vurdering ON vurdering.SYKDOM_VURDERINGER_ID = vurderinger.ID
            JOIN BEHANDLING behandling ON grunnlag.BEHANDLING_ID = behandling.ID
            LEFT JOIN AVBRYT_REVURDERING_GRUNNLAG AR ON behandling.ID = AR.BEHANDLING_ID
            WHERE grunnlag.AKTIV AND behandling.SAK_ID = ?            
                AND behandling.opprettet_tid < (SELECT a.opprettet_tid from behandling a where a.id = ?)
                AND AR.BEHANDLING_ID IS NULL
            """.trimIndent()

        return connection.queryList(query) {
            setParams {
                setLong(1, sakId.id)
                setLong(2, behandlingId.id)
            }
            setRowMapper(::sykdomsvurderingRowmapper)
        }
    }

    private data class Kandidat(
        val sakId: SakId,
        val grunnlagId: Long,
        val behandlingId: BehandlingId,
        val rettighetsperiode: Periode,
        val vurderingerId: Long,
        val grunnlagOpprettetTid: LocalDateTime,
    )

    // Vurdering minus opprettet, id, vurdertIBehandling
    data class SammenlignbarSykdomsvurdering(
        val begrunnelse: String,
        val vurderingenGjelderFra: LocalDate?, // TODO: Gjør påkrevd etter migrering
        val vurderingenGjelderTil: LocalDate?,
        val dokumenterBruktIVurdering: List<JournalpostId>,
        val harSkadeSykdomEllerLyte: Boolean,
        val erSkadeSykdomEllerLyteVesentligdel: Boolean?,
        val erNedsettelseIArbeidsevneAvEnVissVarighet: Boolean?,
        val erNedsettelseIArbeidsevneMerEnnHalvparten: Boolean?,
        val erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense: Boolean?,
        val yrkesskadeBegrunnelse: String?,
        val erArbeidsevnenNedsatt: Boolean?,
        val kodeverk: String? = null,
        val hoveddiagnose: String? = null,
        val bidiagnoser: List<String>? = emptyList(),
        val vurdertAv: Bruker,
    )

    private fun Sykdomsvurdering.tilSammenlignbar(): SammenlignbarSykdomsvurdering {
        return SammenlignbarSykdomsvurdering(
            begrunnelse = this.begrunnelse,
            vurderingenGjelderFra = this.vurderingenGjelderFra,
            vurderingenGjelderTil = this.vurderingenGjelderTil,
            dokumenterBruktIVurdering = this.dokumenterBruktIVurdering,
            harSkadeSykdomEllerLyte = this.harSkadeSykdomEllerLyte,
            erSkadeSykdomEllerLyteVesentligdel = this.erSkadeSykdomEllerLyteVesentligdel,
            erNedsettelseIArbeidsevneAvEnVissVarighet = this.erNedsettelseIArbeidsevneAvEnVissVarighet,
            erNedsettelseIArbeidsevneMerEnnHalvparten = this.erNedsettelseIArbeidsevneMerEnnHalvparten,
            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = this.erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense,
            yrkesskadeBegrunnelse = this.yrkesskadeBegrunnelse,
            erArbeidsevnenNedsatt = this.erArbeidsevnenNedsatt,
            kodeverk = this.kodeverk,
            hoveddiagnose = this.hoveddiagnose,
            bidiagnoser = this.bidiagnoser,
            vurdertAv = this.vurdertAv,
        )
    }

    private data class VurderingMedVurderingerId(
        val vurderingerId: Long,
        val sykdomsvurdering: Sykdomsvurdering
    )

    private fun hentVurderinger(vurderingerIds: List<Long>): List<VurderingMedVurderingerId> {
        val query = """
            select  * from sykdom_vurdering
            where sykdom_vurderinger_id = ANY(?::bigint[])
        """.trimIndent()
        return connection.queryList(query) {
            setParams {
                setLongArray(1, vurderingerIds)
            }
            setRowMapper {
                VurderingMedVurderingerId(
                    vurderingerId = it.getLong("sykdom_vurderinger_id"),
                    sykdomsvurdering = sykdomsvurderingRowmapper(it)
                )
            }
        }
    }

    private fun hentKandidater(): List<Kandidat> {
        val kandidaterQuery = """
            select g.id as grunnlag_id,
                     b.id as behandling_id,
                     s.id as sak_id,
                     s.rettighetsperiode,
                     g.sykdom_vurderinger_id,
                     g.opprettet_tid as grunnlag_opprettet_tid
            
            from sykdom_grunnlag g 
            inner join behandling b on g.behandling_id = b.id
            inner join sak s on b.sak_id = s.id
        """.trimIndent()

        return connection.queryList(kandidaterQuery) {
            setRowMapper {
                Kandidat(
                    sakId = SakId(it.getLong("sak_id")),
                    grunnlagId = it.getLong("grunnlag_id"),
                    behandlingId = BehandlingId(it.getLong("behandling_id")),
                    rettighetsperiode = it.getPeriode("rettighetsperiode"), // Denne er teknisk sett feil, men kanskje godt nok. Hvis ikke: join på rettighetsperiode_grunnlag
                    vurderingerId = it.getLong("sykdom_vurderinger_id"),
                    grunnlagOpprettetTid = it.getLocalDateTime("grunnlag_opprettet_tid"),
                )
            }
        }
    }
}

