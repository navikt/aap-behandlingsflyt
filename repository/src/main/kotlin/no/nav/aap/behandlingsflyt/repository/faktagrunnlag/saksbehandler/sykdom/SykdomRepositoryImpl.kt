package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.sykdom

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.ArbeidsevneNedsattValg
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Diagnose
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.ErNedsettelseMerEnnYrkesskadegrenseValg
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.ErNedsettelseMinstHalvpartenValg
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomsvurderingMedId
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.YrkesskadeSak
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Yrkesskadevurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.lookup.repository.Factory
import no.nav.aap.verdityper.dokument.JournalpostId
import org.slf4j.LoggerFactory
import java.time.LocalDate

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
                ER_ARBEIDSEVNE_NEDSATT, 
                HAR_NEDSATT_ARBEIDSEVNE,
                HAR_SYKDOM_SKADE_LYTE,
                ER_SYKDOM_SKADE_LYTE_VESETLING_DEL, ER_NEDSETTELSE_MER_ENN_HALVPARTEN,
                ER_NEDSETTELSE_MER_ENN_YRKESSKADE_GRENSE, ER_NEDSETTELSE_AV_EN_VISS_VARIGHET,
                ER_NEDSETTELSE_MINST_HALVPARTEN, ER_NEDSETTELSE_MER_ENN_YRKESSKADEGRENSE,
                YRKESSKADE_BEGRUNNELSE, KODEVERK,
                DIAGNOSE, OPPRETTET_TID, VURDERT_AV_IDENT, VURDERT_I_BEHANDLING, VURDERINGEN_GJELDER_TIL)
            VALUES
            (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()

        for (vurdering in vurderinger) {
            val id = connection.executeReturnKey(query) {
                setParams {
                    setLong(1, sykdomsvurderingerId)
                    setString(2, vurdering.begrunnelse)
                    setLocalDate(3, vurdering.vurderingenGjelderFra)
                    setBoolean(4, vurdering.erArbeidsevnenNedsatt)
                    setEnumName(5, vurdering.harNedsattArbeidsevne)
                    setBoolean(6, vurdering.harSkadeSykdomEllerLyte)
                    setBoolean(7, vurdering.erSkadeSykdomEllerLyteVesentligdel)
                    setBoolean(8, vurdering.erNedsettelseIArbeidsevneMerEnnHalvparten)
                    setBoolean(9, vurdering.erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense)
                    setBoolean(10, vurdering.erNedsettelseIArbeidsevneAvEnVissVarighet)
                    setEnumName(11, vurdering.erNedsettelseMinstHalvparten)
                    setEnumName(12, vurdering.erNedsettelseMerEnnYrkesskadegrense)
                    setString(13, vurdering.yrkesskadeBegrunnelse)
                    setString(14, vurdering.diagnose?.kodeverk)
                    setString(15, vurdering.diagnose?.hoveddiagnose)
                    setInstant(16, vurdering.opprettet)
                    setString(17, vurdering.vurdertAv.ident)
                    setLong(18, vurdering.vurdertIBehandling.id)
                    setLocalDate(19, vurdering.vurderingenGjelderTil)
                }
            }

            lagreSykdomDokumenter(id, vurdering.dokumenterBruktIVurdering)
            lagreBidiagnose(id, vurdering.diagnose?.bidiagnoser.orEmpty())
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

    private data class SykdomsvurderingRad(
        val id: Long,
        val begrunnelse: String,
        val vurderingenGjelderFra: LocalDate,
        val harSykdomSkadeLyte: Boolean,
        val erSykdomSkadeLyteVesentligdel: Boolean?,
        val erNedsettelseMerEnnHalvparten: Boolean?,
        val erNedsettelseMerEnnYrkesskadeGrense: Boolean?,
        val erNedsettelseAvEnVissVarighet: Boolean?,
        val erNedsettelseMinstHalvparten: ErNedsettelseMinstHalvpartenValg?,
        val erNedsettelseMerEnnYrkesskadegrense: ErNedsettelseMerEnnYrkesskadegrenseValg?,
        val erArbeidsevnenNedsatt: Boolean?,
        val harNedsattArbeidsevne: ArbeidsevneNedsattValg?,
        val yrkesskadeBegrunnelse: String?,
        val kodeverk: String?,
        val diagnose: String?,
        val opprettetTid: java.time.Instant,
        val vurdertAvIdent: String,
        val vurdertIBehandling: Long,
        val vurderingenGjelderTil: LocalDate?,
    )

    private fun mapSykdomsvurderingRad(row: Row): SykdomsvurderingRad = SykdomsvurderingRad(
        id = row.getLong("id"),
        begrunnelse = row.getString("BEGRUNNELSE"),
        vurderingenGjelderFra = row.getLocalDate("VURDERINGEN_GJELDER_FRA"),
        harSykdomSkadeLyte = row.getBoolean("HAR_SYKDOM_SKADE_LYTE"),
        erSykdomSkadeLyteVesentligdel = row.getBooleanOrNull("ER_SYKDOM_SKADE_LYTE_VESETLING_DEL"),
        erNedsettelseMerEnnHalvparten = row.getBooleanOrNull("ER_NEDSETTELSE_MER_ENN_HALVPARTEN"),
        erNedsettelseMerEnnYrkesskadeGrense = row.getBooleanOrNull("ER_NEDSETTELSE_MER_ENN_YRKESSKADE_GRENSE"),
        erNedsettelseAvEnVissVarighet = row.getBooleanOrNull("ER_NEDSETTELSE_AV_EN_VISS_VARIGHET"),
        erNedsettelseMinstHalvparten = row.getEnumOrNull("ER_NEDSETTELSE_MINST_HALVPARTEN"),
        erNedsettelseMerEnnYrkesskadegrense = row.getEnumOrNull("ER_NEDSETTELSE_MER_ENN_YRKESSKADEGRENSE"),
        erArbeidsevnenNedsatt = row.getBooleanOrNull("ER_ARBEIDSEVNE_NEDSATT"),
        harNedsattArbeidsevne = row.getEnumOrNull("HAR_NEDSATT_ARBEIDSEVNE"),
        yrkesskadeBegrunnelse = row.getStringOrNull("YRKESSKADE_BEGRUNNELSE"),
        kodeverk = row.getStringOrNull("KODEVERK"),
        diagnose = row.getStringOrNull("DIAGNOSE"),
        opprettetTid = row.getInstant("OPPRETTET_TID"),
        vurdertAvIdent = row.getString("VURDERT_AV_IDENT"),
        vurdertIBehandling = row.getLong("VURDERT_I_BEHANDLING"),
        vurderingenGjelderTil = row.getLocalDateOrNull("VURDERINGEN_GJELDER_TIL"),
    )

    private fun mapSykdommer(sykdomVurderingerId: Long?): List<Sykdomsvurdering> {
        val rader = connection.queryList(
            """
            SELECT id,
                   BEGRUNNELSE,
                   VURDERINGEN_GJELDER_FRA,
                   HAR_SYKDOM_SKADE_LYTE,
                   ER_SYKDOM_SKADE_LYTE_VESETLING_DEL,
                   ER_NEDSETTELSE_MER_ENN_HALVPARTEN,
                   ER_NEDSETTELSE_MER_ENN_YRKESSKADE_GRENSE,
                   ER_NEDSETTELSE_AV_EN_VISS_VARIGHET,
                   ER_NEDSETTELSE_MINST_HALVPARTEN, 
                   ER_NEDSETTELSE_MER_ENN_YRKESSKADEGRENSE,
                   ER_ARBEIDSEVNE_NEDSATT,
                   HAR_NEDSATT_ARBEIDSEVNE,
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
            setParams { setLong(1, sykdomVurderingerId) }
            setRowMapper(::mapSykdomsvurderingRad)
        }
        return mapSykdomsvurderingRader(rader)
    }

    private fun mapSykdomsvurderingRader(rader: List<SykdomsvurderingRad>): List<Sykdomsvurdering> {
        if (rader.isEmpty()) return emptyList()
        val ids = rader.map { it.id }
        val bidiagnoserMap = hentAlleBidiagnoser(ids)
        val dokumenterMap = hentAlleSykdomsDokumenter(ids)
        return rader.map { rad -> sykdomsvurderingRowmapper(rad, bidiagnoserMap, dokumenterMap) }
    }

    private fun sykdomsvurderingRowmapper(
        rad: SykdomsvurderingRad,
        bidiagnoserMap: Map<Long, List<String>>,
        dokumenterMap: Map<Long, List<JournalpostId>>,
    ): Sykdomsvurdering {
        return Sykdomsvurdering(
            begrunnelse = rad.begrunnelse,
            vurderingenGjelderFra = rad.vurderingenGjelderFra,
            dokumenterBruktIVurdering = dokumenterMap[rad.id].orEmpty(),
            harSkadeSykdomEllerLyte = rad.harSykdomSkadeLyte,
            erSkadeSykdomEllerLyteVesentligdel = rad.erSykdomSkadeLyteVesentligdel,
            erNedsettelseIArbeidsevneMerEnnHalvparten = rad.erNedsettelseMerEnnHalvparten,
            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = rad.erNedsettelseMerEnnYrkesskadeGrense,
            erNedsettelseIArbeidsevneAvEnVissVarighet = rad.erNedsettelseAvEnVissVarighet,
            erArbeidsevnenNedsatt = rad.erArbeidsevnenNedsatt,
            harNedsattArbeidsevne = rad.harNedsattArbeidsevne,
            yrkesskadeBegrunnelse = rad.yrkesskadeBegrunnelse,
            diagnose = rad.kodeverk?.let {
                Diagnose(
                    kodeverk = it,
                    hoveddiagnose = rad.diagnose,
                    bidiagnoser = bidiagnoserMap[rad.id].orEmpty()
                )
            },
            opprettet = rad.opprettetTid,
            vurdertAv = Bruker(rad.vurdertAvIdent),
            vurdertIBehandling = BehandlingId(rad.vurdertIBehandling),
            vurderingenGjelderTil = rad.vurderingenGjelderTil,
            erNedsettelseMinstHalvparten = rad.erNedsettelseMinstHalvparten,
            erNedsettelseMerEnnYrkesskadegrense = rad.erNedsettelseMerEnnYrkesskadegrense
        )
    }

    private fun hentAlleBidiagnoser(vurderingIds: List<Long>): Map<Long, List<String>> {
        if (vurderingIds.isEmpty()) return emptyMap()
        return connection.queryList(
            "SELECT VURDERING_ID, KODE FROM SYKDOM_VURDERING_BIDIAGNOSER WHERE VURDERING_ID = ANY(?::bigint[])"
        ) {
            setParams { setLongArray(1, vurderingIds) }
            setRowMapper { row -> row.getLong("VURDERING_ID") to row.getString("KODE") }
        }.groupBy({ it.first }, { it.second })
    }

    private fun hentAlleSykdomsDokumenter(vurderingIds: List<Long>): Map<Long, List<JournalpostId>> {
        if (vurderingIds.isEmpty()) return emptyMap()
        return connection.queryList(
            "SELECT VURDERING_ID, JOURNALPOST FROM SYKDOM_VURDERING_DOKUMENTER WHERE VURDERING_ID = ANY(?::bigint[])"
        ) {
            setParams { setLongArray(1, vurderingIds) }
            setRowMapper { row -> row.getLong("VURDERING_ID") to JournalpostId(row.getString("JOURNALPOST")) }
        }.groupBy({ it.first }, { it.second })
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

        val rader = connection.queryList(query) {
            setParams {
                setLong(1, sakId.id)
                setLong(2, behandlingId.id)
            }
            setRowMapper(::mapSykdomsvurderingRad)
        }
        return mapSykdomsvurderingRader(rader)
    }

    override fun hentBehandlingIderMedUmigrerteSykdomsvurderinger(sisteBehandlingId: Long): List<BehandlingId> {
        val query = """
            SELECT DISTINCT sg.behandling_id
            FROM sykdom_grunnlag sg
            JOIN sykdom_vurdering sv ON sv.sykdom_vurderinger_id = sg.sykdom_vurderinger_id
            JOIN behandling b on b.id = sg.behandling_id
            JOIN sak s on s.id = b.sak_id
            WHERE sg.aktiv = TRUE
              AND sg.behandling_id > ?
              AND sv.er_nedsettelse_minst_halvparten IS NULL
              AND sv.er_nedsettelse_mer_enn_yrkesskadegrense IS NULL
              AND s.opprettet_tid >= ?
            ORDER BY sg.behandling_id
            LIMIT 1
        """.trimIndent()

        return connection.queryList(query) {
            setParams {
                setLong(1, sisteBehandlingId)
                setLocalDate(2, if (Miljø.erDev()) LocalDate.parse("2025-04-01") else LocalDate.parse("2020-01-01"))
            }
            setRowMapper { row ->
                BehandlingId(row.getLong("behandling_id"))
            }
        }
    }

    override fun oppdaterNyeFelter(
        sykdomVurderingId: Long,
        erNedsettelseMinstHalvparten: ErNedsettelseMinstHalvpartenValg?,
        erNedsettelseMerEnnYrkesskadegrense: ErNedsettelseMerEnnYrkesskadegrenseValg?
    ) {
        val query = """
            UPDATE sykdom_vurdering
            SET er_nedsettelse_minst_halvparten = ?,
                er_nedsettelse_mer_enn_yrkesskadegrense = ?
            WHERE id = ?
        """.trimIndent()

        connection.execute(query) {
            setParams {
                setEnumName(1, erNedsettelseMinstHalvparten)
                setEnumName(2, erNedsettelseMerEnnYrkesskadegrense)
                setLong(3, sykdomVurderingId)
            }
        }
    }

    override fun hentSykdomsvurderingMedId(behandlingId: BehandlingId): List<SykdomsvurderingMedId> {
        val sykdomVurderingerIds = getSykdomVurderingerIds(behandlingId)
        
        val rader = connection.queryList(
            """
            SELECT id,
                   BEGRUNNELSE,
                   VURDERINGEN_GJELDER_FRA,
                   HAR_SYKDOM_SKADE_LYTE,
                   ER_SYKDOM_SKADE_LYTE_VESETLING_DEL,
                   ER_NEDSETTELSE_MER_ENN_HALVPARTEN,
                   ER_NEDSETTELSE_MER_ENN_YRKESSKADE_GRENSE,
                   ER_NEDSETTELSE_AV_EN_VISS_VARIGHET,
                   ER_NEDSETTELSE_MINST_HALVPARTEN, 
                   ER_NEDSETTELSE_MER_ENN_YRKESSKADEGRENSE,
                   ER_ARBEIDSEVNE_NEDSATT,
                   HAR_NEDSATT_ARBEIDSEVNE,
                   YRKESSKADE_BEGRUNNELSE,
                   KODEVERK,
                   DIAGNOSE,
                   OPPRETTET_TID,
                   VURDERT_AV_IDENT,
                   VURDERT_I_BEHANDLING,
                   VURDERINGEN_GJELDER_TIL
            FROM SYKDOM_VURDERING
            WHERE SYKDOM_VURDERINGER_ID = ANY(?::bigint[])
            """.trimIndent()
        ) {
            setParams { setLongArray(1, sykdomVurderingerIds) }
            setRowMapper(::mapSykdomsvurderingRad)
        }
        val ids = rader.map { it.id }
        val bidiagnoserMap = hentAlleBidiagnoser(ids)
        val dokumenterMap = hentAlleSykdomsDokumenter(ids)
        return rader.map { rad ->
            SykdomsvurderingMedId(
                id = rad.id,
                sykdomsvurdering = sykdomsvurderingRowmapper(rad, bidiagnoserMap, dokumenterMap)
            )
        }
    }

}

