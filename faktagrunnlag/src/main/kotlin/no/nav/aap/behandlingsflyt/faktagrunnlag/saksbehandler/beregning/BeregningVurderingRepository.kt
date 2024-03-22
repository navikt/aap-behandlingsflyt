package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.dbconnect.Row
import no.nav.aap.verdityper.Beløp
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

class BeregningVurderingRepository(private val connection: DBConnection) {

    private fun mapVurdering(row: Row): BeregningVurdering {
        return BeregningVurdering(
            row.getString("BEGRUNNELSE"),
            row.getLocalDateOrNull("YTTERLIGERE_NEDSATT_ARBEIDSEVNE_DATO"),
            row.getBigDecimalOrNull("YRKESSKADE_ANTATT_ARLIG_INNTEKT")?.let(::Beløp)
        )
    }

    fun hentHvisEksisterer(behandlingId: BehandlingId): BeregningVurdering? {
        val query = """
            SELECT v.BEGRUNNELSE, v.YTTERLIGERE_NEDSATT_ARBEIDSEVNE_DATO, v.YRKESSKADE_ANTATT_ARLIG_INNTEKT 
            FROM BEREGNINGSTIDSPUNKT_GRUNNLAG g 
            INNER JOIN BEREGNINGSTIDSPUNKT_VURDERING v ON g.VURDERING_ID = v.ID 
            WHERE BEHANDLING_ID = ? AND AKTIV = TRUE
        """.trimIndent()
        return connection.queryFirstOrNull(query) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper(::mapVurdering)
        }
    }

    fun hent(behandlingId: BehandlingId): BeregningVurdering {
        return requireNotNull(hentHvisEksisterer(behandlingId))
    }

    fun lagre(behandlingId: BehandlingId, vurdering: BeregningVurdering?) {
        val eksisterendeVurdering = hentHvisEksisterer(behandlingId)

        if (eksisterendeVurdering != vurdering) {
            if (eksisterendeVurdering != null) {
                deaktiverEksisterende(behandlingId)
            }

            val vurderingId = lagreVurdering(vurdering)
            val query = """
            INSERT INTO BEREGNINGSTIDSPUNKT_GRUNNLAG (BEHANDLING_ID, VURDERING_ID) VALUES (?, ?)
        """.trimIndent()

            connection.execute(query) {
                setParams {
                    setLong(1, behandlingId.toLong())
                    setLong(2, vurderingId)
                }
            }
        }
    }

    private fun deaktiverEksisterende(behandlingId: BehandlingId) {
        connection.execute("UPDATE BEREGNINGSTIDSPUNKT_GRUNNLAG SET AKTIV = FALSE WHERE BEHANDLING_ID = ? AND AKTIV = TRUE") {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setResultValidator { require(it == 1) }
        }
    }

    fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        val eksisterendeGrunnlag = hentHvisEksisterer(fraBehandling)
        if (eksisterendeGrunnlag == null) {
            return
        }
        val query = """
            INSERT INTO BEREGNINGSTIDSPUNKT_GRUNNLAG (BEHANDLING_ID, VURDERING_ID) SELECT ?, VURDERING_ID FROM BEREGNINGSTIDSPUNKT_GRUNNLAG WHERE BEHANDLING_ID = ? AND AKTIV
        """.trimIndent()

        connection.execute(query) {
            setParams {
                setLong(1, tilBehandling.toLong())
                setLong(2, fraBehandling.toLong())
            }
        }
    }

    private fun lagreVurdering(vurdering: BeregningVurdering?): Long? {
        if (vurdering == null) {
            return null
        }

        val query = """
            INSERT INTO BEREGNINGSTIDSPUNKT_VURDERING 
            (BEGRUNNELSE, YTTERLIGERE_NEDSATT_ARBEIDSEVNE_DATO, YRKESSKADE_ANTATT_ARLIG_INNTEKT)
            VALUES
            (?, ?, ?)
        """.trimIndent()

        val id = connection.executeReturnKey(query) {
            setParams {
                setString(1, vurdering.begrunnelse)
                setLocalDate(2, vurdering.ytterligereNedsattArbeidsevneDato)
                setBigDecimal(3, vurdering.antattÅrligInntekt?.verdi())
            }
        }

        return id
    }
}