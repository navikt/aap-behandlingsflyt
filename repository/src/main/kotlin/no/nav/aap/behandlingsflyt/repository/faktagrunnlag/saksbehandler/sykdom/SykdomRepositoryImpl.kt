package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.sykdom

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Yrkesskadevurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.lookup.repository.Factory
import no.nav.aap.verdityper.dokument.JournalpostId
import java.time.LocalDateTime

class SykdomRepositoryImpl(private val connection: DBConnection) : SykdomRepository {

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
            null,
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
        behandlingId: BehandlingId,
        yrkesskadevurdering: Yrkesskadevurdering?
    ) {
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)
        val nyttGrunnlag = SykdomGrunnlag(
            null,
            yrkesskadevurdering = yrkesskadevurdering,
            sykdomsvurderinger = eksisterendeGrunnlag?.sykdomsvurderinger ?: emptyList(),
        )

        if (eksisterendeGrunnlag != nyttGrunnlag) {
            eksisterendeGrunnlag?.let {
                deaktiverGrunnlag(behandlingId)
            }

            lagre(behandlingId, nyttGrunnlag)
        }
    }

    private fun lagre(behandlingId: BehandlingId, nyttGrunnlag: SykdomGrunnlag) {
        val (sykdomsvurderingId, sykdomsvurderingerId) = lagreSykdom(nyttGrunnlag.sykdomsvurderinger)
        val yrkesskadeId = lagreYrkesskade(nyttGrunnlag.yrkesskadevurdering)

        val query = """
            INSERT INTO SYKDOM_GRUNNLAG (BEHANDLING_ID, YRKESSKADE_ID, SYKDOM_ID, SYKDOM_VURDERINGER_ID) VALUES (?, ?, ?, ?)
        """.trimIndent()

        connection.execute(query) {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, yrkesskadeId)
                setLong(3, sykdomsvurderingId)
                setLong(4, sykdomsvurderingerId)
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
            (BEGRUNNELSE, ARSAKSSAMMENHENG, ANDEL_AV_NEDSETTELSE)
            VALUES
            (?, ?, ?)
        """.trimIndent()

        val id = connection.executeReturnKey(query) {
            setParams {
                setString(1, vurdering.begrunnelse)
                setBoolean(2, vurdering.erÅrsakssammenheng)
                setInt(3, vurdering.andelAvNedsettelsen?.prosentverdi())
            }
        }

        connection.executeBatch(
            """
            INSERT INTO YRKESSKADE_RELATERTE_SAKER (vurdering_id, referanse) VALUES (?, ?)
        """.trimIndent(), vurdering.relevanteSaker
        ) {
            setParams { sak ->
                setLong(1, id)
                setString(2, sak)
            }
        }

        return id
    }

    /* litt funky returtype til migrering er ferdig */
    private fun lagreSykdom(vurderinger: List<Sykdomsvurdering>): Pair<Long?, Long> {
        val sykdomsvurderingerId = connection.executeReturnKey("""INSERT INTO SYKDOM_VURDERINGER DEFAULT VALUES""")

        val query = """
            INSERT INTO SYKDOM_VURDERING (
                SYKDOM_VURDERINGER_ID,
                BEGRUNNELSE, VURDERINGEN_GJELDER_FRA,
                ER_ARBEIDSEVNE_NEDSATT, HAR_SYKDOM_SKADE_LYTE,
                ER_SYKDOM_SKADE_LYTE_VESETLING_DEL, ER_NEDSETTELSE_MER_ENN_HALVPARTEN,
                ER_NEDSETTELSE_MER_ENN_YRKESSKADE_GRENSE, ER_NEDSETTELSE_AV_EN_VISS_VARIGHET,
                YRKESSKADE_BEGRUNNELSE, KODEVERK,
                DIAGNOSE, OPPRETTET_TID)
            VALUES
            (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()

        var id: Long? = null
        for (vurdering in vurderinger) {
            id = connection.executeReturnKey(query) {
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
                    setLocalDateTime(13, vurdering.opprettet ?: LocalDateTime.now())
                }
            }

            vurdering.dokumenterBruktIVurdering.forEach {
                lagreSykdomDokument(id, it)
            }

            vurdering.bidiagnoser?.forEach {
                lagreBidiagnose(id, it)
            }
        }

        return Pair(id, sykdomsvurderingerId)
    }

    private fun lagreBidiagnose(sykdomsId: Long, kode: String) {
        val query = """
            INSERT INTO SYKDOM_VURDERING_BIDIAGNOSER (VURDERING_ID, KODE) 
            VALUES (?, ?)
        """.trimIndent()

        connection.execute(query) {
            setParams {
                setLong(1, sykdomsId)
                setString(2, kode)
            }
        }
    }


    private fun lagreSykdomDokument(sykdomsId: Long, journalpostId: JournalpostId) {
        val query = """
            INSERT INTO SYKDOM_VURDERING_DOKUMENTER (VURDERING_ID, JOURNALPOST) 
            VALUES (?, ?)
        """.trimIndent()

        connection.execute(query) {
            setParams {
                setLong(1, sykdomsId)
                setString(2, journalpostId.identifikator)
            }
        }
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        val eksisterendeGrunnlag = hentHvisEksisterer(fraBehandling)
        if (eksisterendeGrunnlag == null) {
            return
        }
        val query = """
            INSERT INTO SYKDOM_GRUNNLAG (BEHANDLING_ID, YRKESSKADE_ID, SYKDOM_ID, SYKDOM_VURDERINGER_ID)
            SELECT ?, YRKESSKADE_ID, SYKDOM_ID, SYKDOM_VURDERINGER_ID
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
            row.getLong("ID"),
            mapYrkesskade(row.getLongOrNull("YRKESSKADE_ID")),
            mapSykdommer(
                sykdomId = row.getLongOrNull("SYKDOM_ID"),
                sykdomVurderingerId = row.getLongOrNull("SYKDOM_VURDERINGER_ID"),
            ),
        )
    }

    private fun mapSykdommer(sykdomId: Long?, sykdomVurderingerId: Long?): List<Sykdomsvurdering> {
        if (sykdomVurderingerId == null) {
            return listOfNotNull(mapSykdom(sykdomId))
        }

        return connection.queryList(
        """
            SELECT id, BEGRUNNELSE, VURDERINGEN_GJELDER_FRA, HAR_SYKDOM_SKADE_LYTE, ER_SYKDOM_SKADE_LYTE_VESETLING_DEL, ER_NEDSETTELSE_MER_ENN_HALVPARTEN, ER_NEDSETTELSE_MER_ENN_YRKESSKADE_GRENSE, ER_NEDSETTELSE_AV_EN_VISS_VARIGHET, ER_ARBEIDSEVNE_NEDSATT, YRKESSKADE_BEGRUNNELSE, KODEVERK, DIAGNOSE, OPPRETTET_TID
            FROM SYKDOM_VURDERING WHERE SYKDOM_VURDERINGER_ID = ?
            """.trimIndent()
        ) {
            setParams {
                setLong(1, sykdomVurderingerId)
            }
            setRowMapper(::sykdomsvurderingRowmapper)
        }
    }


    private fun mapSykdom(sykdomId: Long?): Sykdomsvurdering? {
        if (sykdomId == null) {
            return null
        }
        return connection.queryFirstOrNull(
            """
            SELECT id, BEGRUNNELSE, VURDERINGEN_GJELDER_FRA, HAR_SYKDOM_SKADE_LYTE, ER_SYKDOM_SKADE_LYTE_VESETLING_DEL, ER_NEDSETTELSE_MER_ENN_HALVPARTEN, ER_NEDSETTELSE_MER_ENN_YRKESSKADE_GRENSE, ER_NEDSETTELSE_AV_EN_VISS_VARIGHET, ER_ARBEIDSEVNE_NEDSATT, YRKESSKADE_BEGRUNNELSE, KODEVERK, DIAGNOSE, OPPRETTET_TID
            FROM SYKDOM_VURDERING WHERE id = ?
            """.trimIndent()
        ) {
            setParams {
                setLong(1, sykdomId)
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
            opprettet = row.getLocalDateTimeOrNull("OPPRETTET_TID"),
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
            SELECT id, BEGRUNNELSE, ARSAKSSAMMENHENG, ANDEL_AV_NEDSETTELSE
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
                    relevanteSaker = hentRelevanteSaker(id)
                )
            }
        }
    }

    private fun hentRelevanteSaker(vurderingId: Long): List<String> {
        val query = """
            SELECT * FROM YRKESSKADE_RELATERTE_SAKER WHERE vurdering_id = ?
        """.trimIndent()
        return connection.queryList(query) {
            setParams {
                setLong(1, vurderingId)
            }
            setRowMapper { row ->
                row.getString("REFERANSE")
            }
        }
    }

    override fun hent(behandlingId: BehandlingId): SykdomGrunnlag {
        return requireNotNull(hentHvisEksisterer(behandlingId))
    }
}
