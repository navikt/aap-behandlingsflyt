package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.verdityper.dokument.JournalpostId
import no.nav.aap.verdityper.sakogbehandling.BehandlingId


class SoningRepository(private val connection: DBConnection) {

    fun lagre(behandlkingId: BehandlingId, soningsvurdering: Soningsvurdering) {
        settSoningsvurderingInnaktiv(behandlkingId)
        val vurderingId = connection.executeReturnKey("""INSERT INTO SONINGSFORHOLD_VURDERING 
            (BEHANDLING_ID, 
            SONING_UTENFOR_FENGSEL,
             BEGRUNNELSE, 
             ARBEID_UTENFOR_ANSALT,
             FORSTE_ARBEIDSDAG)
             VALUES (?, ?, ?, ?, ?)""".trimMargin()) {
            setParams {
                setLong(1, behandlkingId.toLong())
                setBoolean(2, soningsvurdering.soningUtenforFengsel)
                setString(3, soningsvurdering.begrunnelse)
                setBoolean(4, soningsvurdering.arbeidUtenforAnstalt)
                setLocalDate(5, soningsvurdering.førsteArbeidsdag)
            }
        }

        soningsvurdering.dokumenterBruktIVurdering.forEach { lagreDokument(vurderingId, it) }
    }

    fun settSoningsvurderingInnaktiv(behandlkingId: BehandlingId) {
        hentAktivSoningsvurderingHvisEksisterer(behandlkingId)
        connection.execute("""
            UPDATE SONINGSFORHOLD_VURDERING SET AKTIV = FALSE 
            WHERE BEHANDLING_ID = ?""".trimMargin()) {
            setParams {
                setLong(1, behandlkingId.toLong())
            }
        }
    }

    fun hentAktiveSoningsvurdering(behandlkingId: BehandlingId): Soningsvurdering {
        return connection.queryFirst(
            """
                SELECT * FROM SONINGSFORHOLD_VURDERING
                WHERE BEHANDLING_ID = ? 
                    AND AKTIV = TRUE
            """.trimIndent(), {
                setParams {
                    setLong(1, behandlkingId.toLong())
                }
                setRowMapper{
                    mapSoningsvurdering(it)
                }
            }
        )
    }

    fun hentAktivSoningsvurderingHvisEksisterer(behandlingId: BehandlingId): Soningsvurdering? {
        return try {
            hentAktiveSoningsvurdering(behandlingId)
        } catch (e: NoSuchElementException) { null }
    }

    // TODO dokumenter knyttet til vurdering er en kilde til høy kodeduplisering, vurder regularisering av modell
    private fun lagreDokument(vurderingId: Long, journalpostId: JournalpostId) {
        val query = """
            INSERT INTO SONINGSFORHOLD_VURDERING_DOKUMENTER (vurdering_id, journalpost) 
            VALUES (?, ?)
        """.trimIndent()

        connection.execute(query) {
            setParams {
                setLong(1, vurderingId)
                setString(2, journalpostId.identifikator)
            }
        }
    }

    private fun mapSoningsvurdering(row: Row): Soningsvurdering {
        return Soningsvurdering(
            dokumenterBruktIVurdering = hentDokumenterTilVurdering(row.getLong("ID")),
            soningUtenforFengsel = row.getBoolean("SONING_UTENFOR_FENGSEL"),
            begrunnelse = row.getStringOrNull("BEGRUNNELSE"),
            arbeidUtenforAnstalt = row.getBooleanOrNull("ARBEID_UTENFOR_ANSALT"),
            førsteArbeidsdag = row.getLocalDateOrNull("FORSTE_ARBEIDSDAG")
        )
    }

    private fun hentDokumenterTilVurdering(studentId: Long): List<JournalpostId> {
        val query = """
            SELECT journalpost FROM SONINGSFORHOLD_VURDERING_DOKUMENTER WHERE vurdering_id = ?
        """.trimIndent()

        return connection.queryList(query) {
            setParams {
                setLong(1, studentId)
            }
            setRowMapper {
                JournalpostId(it.getString("journalpost"))
            }
        }
    }

}