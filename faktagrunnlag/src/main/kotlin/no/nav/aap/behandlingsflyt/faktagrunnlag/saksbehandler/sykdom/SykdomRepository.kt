package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.verdityper.dokument.JournalpostId
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

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
        yrkesskadevurdering: Yrkesskadevurdering?
    ) {
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)
        val nyttGrunnlag = SykdomGrunnlag(
            null,
            yrkesskadevurdering = yrkesskadevurdering,
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
            (BEGRUNNELSE, ARSAKSSAMMENHENG, SKADEDATO, ANDEL_AV_NEDSETTELSE)
            VALUES
            (?, ?, ?, ?)
        """.trimIndent()

        val id = connection.executeReturnKey(query) {
            setParams {
                setString(1, vurdering.begrunnelse)
                setBoolean(2, vurdering.erÅrsakssammenheng)
                setLocalDate(3, vurdering.skadetidspunkt)
                setInt(4, vurdering.andelAvNedsettelse?.prosentverdi())
            }
        }

        return id
    }

    private fun lagreSykdom(vurdering: Sykdomsvurdering?): Long? {
        if (vurdering == null) {
            return null
        }

        val query = """
            INSERT INTO SYKDOM_VURDERING 
            (BEGRUNNELSE, ER_ARBEIDSEVNE_NEDSATT, HAR_SYKDOM_SKADE_LYTE, ER_SYKDOM_SKADE_LYTE_VESETLING_DEL, ER_NEDSETTELSE_HOYERE_ENN_NEDRE_GRENSE, NEDRE_GRENSE, NEDSATT_ARBEIDSEVNE_DATO)
            VALUES
            (?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()

        val id = connection.executeReturnKey(query) {
            setParams {
                setString(1, vurdering.begrunnelse)
                setBoolean(2, vurdering.erArbeidsevnenNedsatt)
                setBoolean(3, vurdering.harSkadeSykdomEllerLyte)
                setBoolean(4, vurdering.erSkadeSykdomEllerLyteVesentligdel)
                setBoolean(5, vurdering.erNedsettelseIArbeidsevneHøyereEnnNedreGrense)
                setEnumName(6, vurdering.nedreGrense)
                setLocalDate(7, vurdering.nedsattArbeidsevneDato)
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
            SELECT BEGRUNNELSE, HAR_SYKDOM_SKADE_LYTE, ER_SYKDOM_SKADE_LYTE_VESETLING_DEL, ER_NEDSETTELSE_HOYERE_ENN_NEDRE_GRENSE, NEDRE_GRENSE, NEDSATT_ARBEIDSEVNE_DATO, ER_ARBEIDSEVNE_NEDSATT
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
                    row.getBoolean("HAR_SYKDOM_SKADE_LYTE"),
                    row.getBooleanOrNull("ER_SYKDOM_SKADE_LYTE_VESETLING_DEL"),
                    row.getBooleanOrNull("ER_NEDSETTELSE_HOYERE_ENN_NEDRE_GRENSE"),
                    row.getEnumOrNull("NEDRE_GRENSE"),
                    row.getLocalDateOrNull("NEDSATT_ARBEIDSEVNE_DATO"),
                    row.getBooleanOrNull("ER_ARBEIDSEVNE_NEDSATT")
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
            SELECT BEGRUNNELSE, ARSAKSSAMMENHENG, SKADEDATO, ANDEL_AV_NEDSETTELSE
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
                    row.getBoolean("ARSAKSSAMMENHENG"),
                    row.getLocalDateOrNull("SKADEDATO"),
                    row.getIntOrNull("ANDEL_AV_NEDSETTELSE")?.let(::Prosent)
                )
            }
        }
    }

    fun hent(behandlingId: BehandlingId): SykdomGrunnlag {
        return requireNotNull(hentHvisEksisterer(behandlingId))
    }
}
