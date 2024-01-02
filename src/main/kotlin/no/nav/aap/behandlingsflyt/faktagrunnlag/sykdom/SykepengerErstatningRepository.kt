package no.nav.aap.behandlingsflyt.faktagrunnlag.sykdom

import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.sykdom.SykepengerVurdering
import no.nav.aap.behandlingsflyt.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.behandling.dokumenter.JournalpostId
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection

class SykepengerErstatningRepository(private val connection: DBConnection) {

    fun lagre(behandlingId: BehandlingId, vurdering: SykepengerVurdering?) {
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)

        val nyttGrunnlag = SykepengerErstatningGrunnlag(vurdering = vurdering)

        if (eksisterendeGrunnlag != nyttGrunnlag) {
            eksisterendeGrunnlag?.let {
                deaktiverGrunnlag(behandlingId)
            }

            lagreGrunnlag(behandlingId, nyttGrunnlag)
        }
    }

    private fun lagreGrunnlag(behandlingId: BehandlingId, nyttGrunnlag: SykepengerErstatningGrunnlag) {
        val vurdering: SykepengerVurdering? = nyttGrunnlag.vurdering
        var vurderingId: Long? = null
        if (vurdering != null) {
            val query = """
            INSERT INTO SYKEPENGE_VURDERING (begrunnelse, oppfylt) VALUES (?, ?)
        """.trimIndent()

            vurderingId = connection.executeReturnKey(query) {
                setParams {
                    setString(1, vurdering.begrunnelse)
                    setBoolean(2, vurdering.harRettPå)
                }
            }

            vurdering.dokumenterBruktIVurdering.forEach {
                lagreDokument(vurderingId, it)
            }
        }
        val grunnlagQuery = """
            INSERT INTO SYKEPENGE_ERSTATNING_GRUNNLAG (behandling_id, vurdering_id) VALUES (?, ?)
        """.trimIndent()

        connection.execute(grunnlagQuery) {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, vurderingId)
            }
        }
    }

    private fun lagreDokument(vurderingId: Long, journalpostId: JournalpostId) {
        val query = """
            INSERT INTO SYKEPENGE_VURDERING_DOKUMENTER (vurdering_id, journalpost) 
            VALUES (?, ?)
        """.trimIndent()

        connection.execute(query) {
            setParams {
                setLong(1, vurderingId)
                setString(2, journalpostId.identifikator)
            }
        }
    }

    private fun deaktiverGrunnlag(behandlingId: BehandlingId) {
        connection.execute("UPDATE SYKEPENGE_ERSTATNING_GRUNNLAG set aktiv = false WHERE behandling_id = ? and aktiv = true") {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setResultValidator { require(it == 1) }
        }
    }

    fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        val hentHvisEksisterer = hentHvisEksisterer(fraBehandling)
        if (hentHvisEksisterer == null) {
            return
        }

        val query = """
            INSERT INTO SYKEPENGE_ERSTATNING_GRUNNLAG (behandling_id, vurdering_id) 
            SELECT ?, vurdering_id from SYKEPENGE_ERSTATNING_GRUNNLAG where behandling_id = ? and aktiv
        """.trimIndent()

        connection.execute(query) {
            setParams {
                setLong(1, tilBehandling.toLong())
                setLong(2, fraBehandling.toLong())
            }
        }
    }

    fun hentHvisEksisterer(behandlingId: BehandlingId): SykepengerErstatningGrunnlag? {
        val query = """
            SELECT * FROM SYKEPENGE_ERSTATNING_GRUNNLAG WHERE behandling_id = ? and aktiv = true
        """.trimIndent()

        return connection.queryFirstOrNull(query) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper { row ->
                SykepengerErstatningGrunnlag(row.getLong("id"), mapVurdering(row.getLongOrNull("vurdering_id")))
            }
        }
    }

    private fun mapVurdering(vurderingId: Long?): SykepengerVurdering? {
        if (vurderingId == null) {
            return null
        }

        val query = """
            SELECT * FROM SYKEPENGE_VURDERING WHERE id = ?
        """.trimIndent()

        return connection.queryFirstOrNull(query) {
            setParams {
                setLong(1, vurderingId)
            }
            setRowMapper { row ->
                SykepengerVurdering(
                    row.getString("begrunnelse"),
                    hentDokumenter(vurderingId),
                    row.getBoolean("oppfylt")
                )
            }
        }
    }

    private fun hentDokumenter(vurderingId: Long): List<JournalpostId> {
        val query = """
            SELECT journalpost FROM SYKEPENGE_VURDERING_DOKUMENTER WHERE vurdering_id = ?
        """.trimIndent()
        return connection.queryList(query) {
            setParams {
                setLong(1, vurderingId)
            }
            setRowMapper { row ->
                JournalpostId(row.getString("journalpost"))
            }
        }
    }

    fun hent(behandlingId: BehandlingId): SykepengerErstatningGrunnlag {
        return requireNotNull(hentHvisEksisterer(behandlingId))
    }
}
