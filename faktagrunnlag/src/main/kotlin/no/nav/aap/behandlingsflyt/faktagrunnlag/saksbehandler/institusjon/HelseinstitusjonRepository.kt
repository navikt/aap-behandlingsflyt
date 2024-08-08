package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.dbconnect.Row
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.flate.HelseinstitusjonVurdering
import no.nav.aap.verdityper.dokument.JournalpostId
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

class HelseinstitusjonRepository(private val connection: DBConnection) {

    fun lagre(behandlingId: BehandlingId, helseinstitusjonVurdering: HelseinstitusjonVurdering) {
        setHelseinstitusjonVurderingInaktiv(behandlingId)
        val vurderingsId = connection.executeReturnKey("""INSERT INTO HELSEINSTITUSJON_VURDERING
            (BEHANDLING_ID,
            BEGRUNNELSE,
            FAAR_KOST_OG_LOSJI,
            HAR_FASTE_UTGIFTER,
            FORSOERGER_EKTEFELLE)
            VALUES(?, ?, ?, ?, ?)
        """.trimMargin()) {
            setParams {
                setLong(1, behandlingId.toLong())
                setString(2, helseinstitusjonVurdering.begrunnelse)
                setBoolean(3, helseinstitusjonVurdering.faarFriKostOgLosji)
                setBoolean(4, helseinstitusjonVurdering.harFasteUtgifter)
                setBoolean(5, helseinstitusjonVurdering.forsoergerEktefelle)
            }
        }

        helseinstitusjonVurdering.dokumenterBruktIVurdering.forEach { lagreDokument(vurderingsId, it) }

    }

    private fun lagreDokument(vurderingId: Long, journalpostId: JournalpostId) {
        val query = """
            INSERT INTO HELSEINSTITUSJON_VURDERING_DOKUMENTER (vurdering_id, journalpost) 
            VALUES (?, ?)
        """.trimIndent()

        connection.execute(query) {
            setParams {
                setLong(1, vurderingId)
                setString(2, journalpostId.identifikator)
            }
        }
    }

    fun setHelseinstitusjonVurderingInaktiv(behandlingId: BehandlingId) {
        hentAktivHelseinstitusjonVurderingHvisEksisterer(behandlingId)
        connection.execute("""
            UPDATE HELSEINSTITUSJON_VURDERING SET AKTIV = FALSE
            WHERE BEHANDLING_ID = ?
        """.trimIndent()) {
            setParams { setLong(1, behandlingId.toLong()) }
        }
    }

    fun hentAktivHelseinstitusjonVurdering(behandlingId: BehandlingId): HelseinstitusjonVurdering {
        return connection.queryFirst(
            """
                SELECT * FROM HELSEINSTITUSJON_VURDERING
                WHERE BEHANDLING_ID = ?
                AND AKTIV = TRUE
            """.trimIndent(), {
                setParams {
                    setLong(1, behandlingId.toLong())
                }
                setRowMapper {
                    mapHelseinstitusjonVurdering(it)
                }
            }
        )
    }

    fun hentAktivHelseinstitusjonVurderingHvisEksisterer(behandlingId: BehandlingId): HelseinstitusjonVurdering? {
        return try {
            hentAktivHelseinstitusjonVurdering(behandlingId)
        } catch (e: NoSuchElementException) { null }
    }

    private fun mapHelseinstitusjonVurdering(row: Row): HelseinstitusjonVurdering {
        return HelseinstitusjonVurdering(
                dokumenterBruktIVurdering = hentDokumenterTilVurdering(row.getLong("ID")),
                begrunnelse = row.getString("BEGRUNNELSE"),
                faarFriKostOgLosji = row.getBoolean("FAAR_KOST_OG_LOSJI"),
                forsoergerEktefelle = row.getBooleanOrNull("FORSOERGER_EKTEFELLE"),
                harFasteUtgifter = row.getBooleanOrNull("HAR_FASTE_UTGIFTER")
                )
    }

    private fun hentDokumenterTilVurdering(studentId: Long): List<JournalpostId> {
        val query = """
            SELECT journalpost FROM HELSEINSTITUSJON_VURDERING_DOKUMENTER WHERE vurdering_id = ?
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