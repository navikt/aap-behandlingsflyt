package no.nav.aap.behandlingsflyt.faktagrunnlag.sykdom

import no.nav.aap.behandlingsflyt.avklaringsbehov.sykdom.NedreGrense
import no.nav.aap.behandlingsflyt.avklaringsbehov.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.avklaringsbehov.sykdom.Yrkesskadevurdering
import no.nav.aap.behandlingsflyt.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.behandling.dokumenter.JournalpostId
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.dbconnect.Row

class SykdomsRepository(private val connection: DBConnection) {

    fun lagre(behandlingId: BehandlingId, yrkesskadevurdering: Yrkesskadevurdering?) {

        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)
        val nyttGrunnlag = SykdomsGrunnlag(
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
        connection.execute("UPDATE SYKDOM_GRUNNLAG set aktiv = false WHERE behandling_id = ? and aktiv = true") {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setResultValidator { require(it == 1) }
        }
    }

    fun lagre(behandlingId: BehandlingId, sykdomsvurdering: Sykdomsvurdering?) {
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)
        val nyttGrunnlag = SykdomsGrunnlag(
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

    private fun lagre(behandlingId: BehandlingId, nyttGrunnlag: SykdomsGrunnlag) {
        val sykdomsvurderingId = lagreSykdom(nyttGrunnlag.sykdomsvurdering)
        val yrkesskadeId = lagreYrkesskade(nyttGrunnlag.yrkesskadevurdering)

        val query = """
            INSERT INTO SYKDOM_GRUNNLAG (behandling_id, yrkesskade_id, sykdom_id) VALUES (?, ?, ?)
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
            (begrunnelse, arsakssammenheng, skadedato)
            VALUES
            (?, ?, ?)
        """.trimIndent()

        val id = connection.executeReturnKey(query) {
            setParams {
                setString(1, vurdering.begrunnelse)
                setBoolean(2, vurdering.erÅrsakssammenheng)
                setLocalDate(3, vurdering.skadedato)
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
            (begrunnelse, er_sykdom_skade_lyte_vesetling_del, er_nedsettelse_høyere_enn_nedre_grense, nedre_grense, nedsettelses_dato)
            VALUES
            (?, ?, ?, ?, ?)
        """.trimIndent()

        val id = connection.executeReturnKey(query) {
            setParams {
                setString(1, vurdering.begrunnelse)
                setBoolean(2, vurdering.erSkadeSykdomEllerLyteVesentligdel)
                setBoolean(3, vurdering.erNedsettelseIArbeidsevneHøyereEnnNedreGrense)
                setEnumName(4, vurdering.nedreGrense)
                setLocalDate(5, vurdering.nedsattArbeidsevneDato)
            }
        }

        vurdering.dokumenterBruktIVurdering.forEach {
            lagreSykdomDokument(id, it)
        }

        return id
    }

    private fun lagreSykdomDokument(sykdomsId: Long, journalpostId: JournalpostId) {
        val query = """
            INSERT INTO SYKDOM_VURDERING_DOKUMENTER (vurdering_id, journalpost) 
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
            INSERT INTO SYKDOM_GRUNNLAG (behandling_id, yrkesskade_id, sykdom_id) VALUES (?, (select yrkesskade_id from SYKDOM_GRUNNLAG where behandling_id = ?), (select sykdom_id from SYKDOM_GRUNNLAG where behandling_id = ?))
        """.trimIndent()

        connection.execute(query) {
            setParams {
                setLong(1, tilBehandling.toLong())
                setLong(2, fraBehandling.toLong())
                setLong(3, fraBehandling.toLong())
            }
        }
    }

    fun hentHvisEksisterer(behandlingId: BehandlingId): SykdomsGrunnlag? {
        val query = """
            SELECT * FROM SYKDOM_GRUNNLAG WHERE behandling_id = ? and aktiv = true
        """.trimIndent()
        return connection.queryFirstOrNull(query) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper(::mapGrunnlag)
        }
    }

    private fun mapGrunnlag(row: Row): SykdomsGrunnlag {
        return SykdomsGrunnlag(
            row.getLong("id"),
            mapYrkesskade(row.getLongOrNull("yrkesskade_id")),
            mapSykdom(row.getLongOrNull("sykdom_id"))
        )
    }

    private fun mapSykdom(sykdomId: Long?): Sykdomsvurdering? {
        if (sykdomId == null) {
            return null
        }
        val query = """
            SELECT * FROM SYKDOM_VURDERING WHERE id = ?
        """.trimIndent()
        return connection.queryFirstOrNull(query) {
            setParams {
                setLong(1, sykdomId)
            }
            setRowMapper { row ->
                Sykdomsvurdering(
                    row.getString("begrunnelse"),
                    hentSykdomsDokumenter(sykdomId),
                    row.getBoolean("er_sykdom_skade_lyte_vesetling_del"),
                    row.getBooleanOrNull("er_nedsettelse_høyere_enn_nedre_grense"),
                    NedreGrense.valueOf(row.getString("nedre_grense")),
                    row.getLocalDateOrNull("nedsettelses_dato")
                )
            }
        }
    }

    private fun hentSykdomsDokumenter(yrkesskadeId: Long): List<JournalpostId> {
        val query = """
            SELECT journalpost FROM SYKDOM_VURDERING_DOKUMENTER WHERE vurdering_id = ?
        """.trimIndent()
        return connection.queryList(query) {
            setParams {
                setLong(1, yrkesskadeId)
            }
            setRowMapper { row ->
                JournalpostId(row.getString("journalpost"))
            }
        }
    }

    private fun mapYrkesskade(yrkesskadeId: Long?): Yrkesskadevurdering? {
        if (yrkesskadeId == null) {
            return null
        }
        val query = """
            SELECT * FROM YRKESSKADE_VURDERING WHERE id = ?
        """.trimIndent()
        return connection.queryFirstOrNull(query) {
            setParams {
                setLong(1, yrkesskadeId)
            }
            setRowMapper { row ->
                Yrkesskadevurdering(
                    row.getString("begrunnelse"),
                    hentYrkesskadeDokumenter(yrkesskadeId),
                    row.getBoolean("arsakssammenheng"),
                    row.getLocalDateOrNull("skadedato")
                )
            }
        }
    }

    private fun hentYrkesskadeDokumenter(yrkesskadeId: Long): List<JournalpostId> {
        val query = """
            SELECT journalpost FROM YRKESSKADE_VURDERING_DOKUMENTER WHERE vurdering_id = ?
        """.trimIndent()
        return connection.queryList(query) {
            setParams {
                setLong(1, yrkesskadeId)
            }
            setRowMapper { row ->
                JournalpostId(row.getString("journalpost"))
            }
        }
    }

    fun hent(behandlingId: BehandlingId): SykdomsGrunnlag {
        return requireNotNull(hentHvisEksisterer(behandlingId))
    }
}
