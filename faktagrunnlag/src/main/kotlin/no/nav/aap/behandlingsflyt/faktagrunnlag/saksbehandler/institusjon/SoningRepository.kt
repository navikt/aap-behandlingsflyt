package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.dbconnect.Row
import no.nav.aap.verdityper.sakogbehandling.BehandlingId


class SoningRepository(private val connection: DBConnection) {

    fun lagre(behandlkingId: BehandlingId, soningsvurdering: Soningsvurdering) {
        settSoningsvurderingInnaktiv(behandlkingId)
        connection.execute("""INSERT INTO SONINGSFORHOLD_GRUNNLAG 
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
    }

    fun settSoningsvurderingInnaktiv(behandlkingId: BehandlingId) {
        hentAktivSoningsvurderingHvisEksisterer(behandlkingId)
        connection.execute("""
            UPDATE SONINGSFORHOLD_GRUNNLAG SET AKTIV = FALSE 
            WHERE BEHANDLING_ID = ?""".trimMargin()) {
            setParams {
                setLong(1, behandlkingId.toLong())
            }
        }
    }

    fun hentAktiveSoningsvurdering(behandlkingId: BehandlingId): Soningsvurdering {
        return connection.queryFirst(
            """
                SELECT * FROM SONINGSFORHOLD_GRUNNLAG
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

    private fun mapSoningsvurdering(row: Row): Soningsvurdering {
        return Soningsvurdering(
            dokumenterBruktIVurdering = emptyList(), // TODO legg til når dokumenter blir lagt til
            soningUtenforFengsel = row.getBoolean("SONING_UTENFOR_FENGSEL"),
            begrunnelseForSoningUtenforAnstalt = row.getStringOrNull("BEGRUNNELSE_FOR_SONING_UTENFOR_FENGSEL"),
            arbeidUtenforAnstalt = row.getBooleanOrNull("ARBEID_UTENFOR_ANSALT"),
            begrunnelseForArbeidUtenforAnstalt = row.getStringOrNull("BEGRUNNELSE_FOR_SONING_UTENFOR_ANSTALT")
        )
    }

}