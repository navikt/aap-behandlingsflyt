package no.nav.aap.behandlingsflyt.faktagrunnlag.sykdom

import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.sykdom.Yrkesskadevurdering
import no.nav.aap.behandlingsflyt.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.behandling.dokumenter.JournalpostId
import no.nav.aap.behandlingsflyt.beregning.Prosent
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.dbconnect.Row
import no.nav.aap.behandlingsflyt.faktagrunnlag.inntekt.Beløp

class SykdomRepository(private val connection: DBConnection) {

    fun lagre(behandlingId: BehandlingId, yrkesskadevurdering: Yrkesskadevurdering?) {

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

    private fun deaktiverGrunnlag(behandlingId: BehandlingId) {
        connection.execute("UPDATE SYKDOM_GRUNNLAG SET AKTIV = FALSE WHERE BEHANDLING_ID = ? AND AKTIV = TRUE") {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setResultValidator { require(it == 1) }
        }
    }

    fun lagre(behandlingId: BehandlingId, sykdomsvurdering: Sykdomsvurdering?) {
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

        val query = """
            INSERT INTO YRKESSKADE_VURDERING 
            (BEGRUNNELSE, ARSAKSSAMMENHENG, SKADEDATO, ANDEL_AV_NEDSETTELSE, ANTATT_ARLIG_INNTEKT)
            VALUES
            (?, ?, ?, ?, ?)
        """.trimIndent()

        val id = connection.executeReturnKey(query) {
            setParams {
                setString(1, vurdering.begrunnelse)
                setBoolean(2, vurdering.erÅrsakssammenheng)
                setLocalDate(3, vurdering.skadetidspunkt)
                setInt(4, vurdering.andelAvNedsettelse?.prosentverdi())
                setBigDecimal(5, vurdering.antattÅrligInntekt?.verdi())
            }
        }

        vurdering.dokumenterBruktIVurdering.forEach {
            lagreYrkesskadeDokument(id, it)
        }

        return id
    }

    private fun lagreYrkesskadeDokument(vurderingId: Long, journalpostId: JournalpostId) {
        val query = """
            INSERT INTO YRKESSKADE_VURDERING_DOKUMENTER (vurdering_id, journalpost) 
            VALUES (?, ?)
        """.trimIndent()

        connection.execute(query) {
            setParams {
                setLong(1, vurderingId)
                setString(2, journalpostId.identifikator)
            }
        }
    }

    private fun lagreSykdom(vurdering: Sykdomsvurdering?): Long? {
        if (vurdering == null) {
            return null
        }

        val query = """
            INSERT INTO SYKDOM_VURDERING 
            (BEGRUNNELSE, ER_SYKDOM_SKADE_LYTE_VESETLING_DEL, ER_NEDSETTELSE_HOYERE_ENN_NEDRE_GRENSE, NEDRE_GRENSE, NEDSATT_ARBEIDSEVNE_DATO, YTTERLIGERE_NEDSATT_ARBEIDSEVNE_DATO)
            VALUES
            (?, ?, ?, ?, ?, ?)
        """.trimIndent()

        val id = connection.executeReturnKey(query) {
            setParams {
                setString(1, vurdering.begrunnelse)
                setBoolean(2, vurdering.erSkadeSykdomEllerLyteVesentligdel)
                setBoolean(3, vurdering.erNedsettelseIArbeidsevneHøyereEnnNedreGrense)
                setEnumName(4, vurdering.nedreGrense)
                setLocalDate(5, vurdering.nedsattArbeidsevneDato)
                setLocalDate(6, vurdering.ytterligereNedsattArbeidsevneDato)
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
            SELECT BEGRUNNELSE, ER_SYKDOM_SKADE_LYTE_VESETLING_DEL, ER_NEDSETTELSE_HOYERE_ENN_NEDRE_GRENSE, NEDRE_GRENSE, NEDSATT_ARBEIDSEVNE_DATO, YTTERLIGERE_NEDSATT_ARBEIDSEVNE_DATO
            FROM SYKDOM_VURDERING WHERE id = ?
            """.trimIndent()
        ) {
            setParams {
                setLong(1, sykdomId)
            }
            setRowMapper { row ->
                Sykdomsvurdering(
                    row.getString("BEGRUNNELSE"),
                    hentSykdomsDokumenter(sykdomId),
                    row.getBoolean("ER_SYKDOM_SKADE_LYTE_VESETLING_DEL"),
                    row.getBooleanOrNull("ER_NEDSETTELSE_HOYERE_ENN_NEDRE_GRENSE"),
                    row.getEnumOrNull("NEDRE_GRENSE"),
                    row.getLocalDateOrNull("NEDSATT_ARBEIDSEVNE_DATO"),
                    row.getLocalDateOrNull("YTTERLIGERE_NEDSATT_ARBEIDSEVNE_DATO")
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
            SELECT BEGRUNNELSE, ARSAKSSAMMENHENG, SKADEDATO, ANDEL_AV_NEDSETTELSE, ANTATT_ARLIG_INNTEKT
            FROM YRKESSKADE_VURDERING
            WHERE ID = ?
        """.trimIndent()
        return connection.queryFirstOrNull(query) {
            setParams {
                setLong(1, yrkesskadeId)
            }
            setRowMapper { row ->
                Yrkesskadevurdering(
                    row.getString("BEGRUNNELSE"),
                    hentYrkesskadeDokumenter(yrkesskadeId),
                    row.getBoolean("ARSAKSSAMMENHENG"),
                    row.getLocalDateOrNull("SKADEDATO"),
                    row.getIntOrNull("ANDEL_AV_NEDSETTELSE")?.let(::Prosent),
                    row.getBigDecimalOrNull("ANTATT_ARLIG_INNTEKT")?.let(::Beløp)
                )
            }
        }
    }

    private fun hentYrkesskadeDokumenter(yrkesskadeId: Long): List<JournalpostId> {
        val query = """
            SELECT JOURNALPOST FROM YRKESSKADE_VURDERING_DOKUMENTER WHERE VURDERING_ID = ?
        """.trimIndent()
        return connection.queryList(query) {
            setParams {
                setLong(1, yrkesskadeId)
            }
            setRowMapper { row ->
                JournalpostId(row.getString("JOURNALPOST"))
            }
        }
    }

    fun hent(behandlingId: BehandlingId): SykdomGrunnlag {
        return requireNotNull(hentHvisEksisterer(behandlingId))
    }
}
