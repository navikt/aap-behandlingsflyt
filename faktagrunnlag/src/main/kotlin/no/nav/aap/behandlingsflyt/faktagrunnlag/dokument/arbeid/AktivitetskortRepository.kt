package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid

import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.dokumenter.Brevkode
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import no.nav.aap.verdityper.sakogbehandling.SakId

class AktivitetskortRepository(private val connection: DBConnection) {
    private val mottattDokumentRepository = MottattDokumentRepository(connection)

    fun hentHvisEksisterer(behandlingId: BehandlingId): AktivitetskortGrunnlag? {
        val query = """
            SELECT * FROM AKTIVITETSKORT_GRUNNLAG WHERE behandling_id = ? and aktiv = true
        """.trimIndent()
        return connection.queryFirstOrNull(query) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper {
                mapGrunnlag(it, behandlingId)
            }
        }
    }

    private fun mapGrunnlag(row: Row, behandlingId: BehandlingId): AktivitetskortGrunnlag {
        val sakId = hentSakId(behandlingId)
        val aktivitetskorteneId = row.getLong("AKTIVITETSKORTENE_ID")

        val query = """
            SELECT * FROM AKTIVITETSKORT WHERE aktivitetskortene_id = ?
        """.trimIndent()

        val aktivitetskortene = connection.queryList(query) {
            setParams {
                setLong(1, aktivitetskorteneId)
            }
            setRowMapper {
                Aktivitetskort(it.getUUID("JOURNALPOST"))
            }
        }.toSet()

        val dokumentRekkefølge = mottattDokumentRepository.hentDokumentRekkefølge(sakId, Brevkode.AKTIVITETSKORT)

        return AktivitetskortGrunnlag(aktivitetskortene, dokumentRekkefølge)
    }

    private fun hentSakId(behandlingId: BehandlingId): SakId {
        val query = """
            SELECT sak_id FROM BEHANDLING WHERE id = ?
        """.trimIndent()

        return connection.queryFirst(query) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper {
                SakId(it.getLong("sak_id"))
            }
        }
    }

    fun lagre(behandlingId: BehandlingId, aktivitetskortene: Set<Aktivitetskort>) {
        val eksisterendeGrunnlag = hentHvisEksisterer(behandlingId)
        val eksisterendeKort = eksisterendeGrunnlag?.aktivitetskortene ?: emptySet()

        if (eksisterendeKort != aktivitetskortene) {
            if (eksisterendeGrunnlag != null) {
                deaktiverGrunnlag(behandlingId)
            }

            lagreNyttGrunnlag(behandlingId, aktivitetskortene)
        }
    }

    private fun lagreNyttGrunnlag(behandlingId: BehandlingId, aktivitetskortene: Set<Aktivitetskort>) {
        val aktivitetskorteneQuery = """
            INSERT INTO AKTIVITETSKORTENE DEFAULT VALUES
            """.trimIndent()
        val aktivitetskorteneId = connection.executeReturnKey(aktivitetskorteneQuery)

        aktivitetskortene.forEach { aktivitetskort ->
            val query = """
            INSERT INTO AKTIVITETSKORT (JOURNALPOST, AKTIVITETSKORTENE_ID) VALUES (?, ?)
            """.trimIndent()
            connection.execute(query) {
                setParams {
                    setUUID(1, aktivitetskort.journalpostId)
                    setLong(2, aktivitetskorteneId)
                }
            }
        }

        val grunnlagQuery = """
            INSERT INTO AKTIVITETSKORT_GRUNNLAG (BEHANDLING_ID, AKTIVITETSKORTENE_ID) VALUES (?, ?)
        """.trimIndent()
        connection.execute(grunnlagQuery) {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, aktivitetskorteneId)
            }
        }
    }

    private fun deaktiverGrunnlag(behandlingId: BehandlingId) {
        connection.execute(
            """UPDATE AKTIVITETSKORT_GRUNNLAG set AKTIV = false WHERE BEHANDLING_ID = ? and AKTIV = true"""
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setResultValidator { require(it == 1) }
        }
    }

    fun kopier(fraBehandlingId: BehandlingId, tilBehandlingId: BehandlingId) {
        hentHvisEksisterer(fraBehandlingId) ?: return
        val query = """
            INSERT INTO AKTIVITETSKORT_GRUNNLAG (BEHANDLING_ID, AKTIVITETSKORTENE_ID) SELECT ?, AKTIVITETSKORTENE_ID from AKTIVITETSKORT_GRUNNLAG where BEHANDLING_ID = ? and AKTIV
        """.trimIndent()

        connection.execute(query) {
            setParams {
                setLong(1, tilBehandlingId.toLong())
                setLong(2, fraBehandlingId.toLong())
            }
        }
    }
}