package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.verdityper.dokument.JournalpostId

class SykdomRepository(private val connection: DBConnection) {

    private fun deaktiverGrunnlag(behandlingId: BehandlingId) {
        connection.execute("UPDATE SYKDOM_GRUNNLAG SET AKTIV = FALSE WHERE BEHANDLING_ID = ? AND AKTIV = TRUE") {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setResultValidator { require(it == 1) }
        }
    }

    fun lagre(
        behandlingId: BehandlingId,
        sykdomsvurdering: Sykdomsvurdering?,
    ) {
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)
        val nyttGrunnlag = SykdomGrunnlag(
            null,
            yrkesskadevurdering = eksisterendeGrunnlag?.yrkesskadevurdering,
            sykdomsvurdering = sykdomsvurdering
        )

        if (eksisterendeGrunnlag != nyttGrunnlag) {
            eksisterendeGrunnlag?.let {
                deaktiverGrunnlag(behandlingId)
            }

            lagre(behandlingId, nyttGrunnlag)
        }
    }

    fun lagre(
        behandlingId: BehandlingId,
        yrkesskadevurdering: Yrkesskadevurdering?
    ) {
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)
        val nyttGrunnlag = SykdomGrunnlag(
            null,
            yrkesskadevurdering = yrkesskadevurdering,
            sykdomsvurdering = eksisterendeGrunnlag?.sykdomsvurdering
        )

        if (eksisterendeGrunnlag != nyttGrunnlag) {
            eksisterendeGrunnlag?.let {
                deaktiverGrunnlag(behandlingId)
            }

            lagre(behandlingId, nyttGrunnlag)
        }
    }

    private fun lagre(behandlingId: BehandlingId, nyttGrunnlag: SykdomGrunnlag) {
        val sykdomsvurderingId = lagreSykdom(nyttGrunnlag.sykdomsvurdering)
        val yrkesskadeId = lagreYrkesskade(nyttGrunnlag.yrkesskadevurdering)

        val query = """
            INSERT INTO SYKDOM_GRUNNLAG (BEHANDLING_ID, YRKESSKADE_ID, SYKDOM_ID) VALUES (?, ?, ?)
        """.trimIndent()

        connection.execute(query) {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, yrkesskadeId)
                setLong(3, sykdomsvurderingId)
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

    private fun lagreSykdom(vurdering: Sykdomsvurdering?): Long? {
        if (vurdering == null) {
            return null
        }
        if (vurdering.id != null) {
            return vurdering.id
        }

        val query = """
            INSERT INTO SYKDOM_VURDERING 
            (BEGRUNNELSE, ER_ARBEIDSEVNE_NEDSATT, HAR_SYKDOM_SKADE_LYTE, ER_SYKDOM_SKADE_LYTE_VESETLING_DEL, ER_NEDSETTELSE_MER_ENN_HALVPARTEN, ER_NEDSETTELSE_MER_ENN_YRKESSKADE_GRENSE, ER_NEDSETTELSE_AV_EN_VISS_VARIGHET, YRKESSKADE_BEGRUNNELSE, KODEVERK, DIAGNOSE)
            VALUES
            (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()

        val id = connection.executeReturnKey(query) {
            setParams {
                setString(1, vurdering.begrunnelse)
                setBoolean(2, vurdering.erArbeidsevnenNedsatt)
                setBoolean(3, vurdering.harSkadeSykdomEllerLyte)
                setBoolean(4, vurdering.erSkadeSykdomEllerLyteVesentligdel)
                setBoolean(5, vurdering.erNedsettelseIArbeidsevneMerEnnHalvparten)
                setBoolean(6, vurdering.erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense)
                setBoolean(7, vurdering.erNedsettelseIArbeidsevneAvEnVissVarighet)
                setString(8, vurdering.yrkesskadeBegrunnelse)
                setString(9, vurdering.kodeverk)
                setString(10, vurdering.diagnose)
            }
        }

        vurdering.dokumenterBruktIVurdering.forEach {
            lagreSykdomDokument(id, it)
        }

        return id
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

    fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        val eksisterendeGrunnlag = hentHvisEksisterer(fraBehandling)
        if (eksisterendeGrunnlag == null) {
            return
        }
        val query = """
            INSERT INTO SYKDOM_GRUNNLAG (BEHANDLING_ID, YRKESSKADE_ID, SYKDOM_ID) SELECT ?, YRKESSKADE_ID, SYKDOM_ID FROM SYKDOM_GRUNNLAG WHERE BEHANDLING_ID = ? AND AKTIV
        """.trimIndent()

        connection.execute(query) {
            setParams {
                setLong(1, tilBehandling.toLong())
                setLong(2, fraBehandling.toLong())
            }
        }
    }

    fun hentHvisEksisterer(behandlingId: BehandlingId): SykdomGrunnlag? {
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
            mapSykdom(row.getLongOrNull("SYKDOM_ID"))
        )
    }

    private fun mapSykdom(sykdomId: Long?): Sykdomsvurdering? {
        if (sykdomId == null) {
            return null
        }
        return connection.queryFirstOrNull(
            """
            SELECT id, BEGRUNNELSE, HAR_SYKDOM_SKADE_LYTE, ER_SYKDOM_SKADE_LYTE_VESETLING_DEL, ER_NEDSETTELSE_MER_ENN_HALVPARTEN, ER_NEDSETTELSE_MER_ENN_YRKESSKADE_GRENSE, ER_NEDSETTELSE_AV_EN_VISS_VARIGHET, ER_ARBEIDSEVNE_NEDSATT, YRKESSKADE_BEGRUNNELSE, KODEVERK, DIAGNOSE
            FROM SYKDOM_VURDERING WHERE id = ?
            """.trimIndent()
        ) {
            setParams {
                setLong(1, sykdomId)
            }
            setRowMapper { row ->
                Sykdomsvurdering(
                    id = row.getLong("id"),
                    begrunnelse = row.getString("BEGRUNNELSE"),
                    dokumenterBruktIVurdering = hentSykdomsDokumenter(sykdomId),
                    harSkadeSykdomEllerLyte = row.getBoolean("HAR_SYKDOM_SKADE_LYTE"),
                    erSkadeSykdomEllerLyteVesentligdel = row.getBooleanOrNull("ER_SYKDOM_SKADE_LYTE_VESETLING_DEL"),
                    erNedsettelseIArbeidsevneMerEnnHalvparten = row.getBooleanOrNull("ER_NEDSETTELSE_MER_ENN_HALVPARTEN"),
                    erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = row.getBooleanOrNull("ER_NEDSETTELSE_MER_ENN_YRKESSKADE_GRENSE"),
                    erNedsettelseIArbeidsevneAvEnVissVarighet = row.getBooleanOrNull("ER_NEDSETTELSE_AV_EN_VISS_VARIGHET"),
                    erArbeidsevnenNedsatt = row.getBooleanOrNull("ER_ARBEIDSEVNE_NEDSATT"),
                    yrkesskadeBegrunnelse = row.getStringOrNull("YRKESSKADE_BEGRUNNELSE"),
                    kodeverk = row.getStringOrNull("KODEVERK"),
                    diagnose = row.getStringOrNull("DIAGNOSE")
                )
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

    fun hent(behandlingId: BehandlingId): SykdomGrunnlag {
        return requireNotNull(hentHvisEksisterer(behandlingId))
    }
}
