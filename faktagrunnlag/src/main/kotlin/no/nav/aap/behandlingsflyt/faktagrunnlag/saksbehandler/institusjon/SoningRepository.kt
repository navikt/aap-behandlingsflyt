package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.dbconnect.Row
import no.nav.aap.verdityper.dokument.JournalpostId
import no.nav.aap.verdityper.sakogbehandling.BehandlingId


class SoningRepository(private val connection: DBConnection) {

    fun lagre(behandlkingId: BehandlingId, soningsvurdering: Soningsvurdering) {
        settSoningsvurderingInnaktiv(behandlkingId)
        val vurderingId = connection.executeReturnKey("""INSERT INTO SONINGSFORHOLD_VURDERING 
            (BEHANDLING_ID, 
            SONING_UTENFOR_FENGSEL,
             BEGRUNNELSE_FOR_SONING_UTENFOR_FENGSEL, 
             ARBEID_UTENFOR_ANSALT, 
             BEGRUNNELSE_FOR_SONING_UTENFOR_ANSTALT) 
             VALUES (?, ?, ?, ?, ?)""".trimMargin()) {
            setParams {
                setLong(1, behandlkingId.toLong())
                setBoolean(2, soningsvurdering.soningUtenforFengsel)
                setString(3, soningsvurdering.begrunnelseForSoningUtenforAnstalt)
                setBoolean(4, soningsvurdering.arbeidUtenforAnstalt)
                setString(5, soningsvurdering.begrunnelseForArbeidUtenforAnstalt)
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
            begrunnelseForSoningUtenforAnstalt = row.getStringOrNull("BEGRUNNELSE_FOR_SONING_UTENFOR_FENGSEL"),
            arbeidUtenforAnstalt = row.getBooleanOrNull("ARBEID_UTENFOR_ANSALT"),
            begrunnelseForArbeidUtenforAnstalt = row.getStringOrNull("BEGRUNNELSE_FOR_SONING_UTENFOR_ANSTALT")
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