package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.verdityper.Beløp
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

class BeregningVurderingRepository(private val connection: DBConnection) {

    private fun mapTidspunktVurdering(vurderingId: Long?): BeregningstidspunktVurdering? {
        if (vurderingId == null) {
            return null
        }
        val query = """
            SELECT NEDSATT_BEGRUNNELSE, NEDSATT_ARBEIDSEVNE_DATO, YTTERLIGERE_NEDSATT_ARBEIDSEVNE_DATO, YTTERLIGERE_NEDSATT_BEGRUNNELSE
            FROM BEREGNINGSTIDSPUNKT_VURDERING
            WHERE id = ?
        """.trimIndent()

        return connection.queryFirst(query) {
            setParams {
                setLong(1, vurderingId)
            }
            setRowMapper { row ->
                BeregningstidspunktVurdering(
                    vurderingId,
                    row.getString("NEDSATT_BEGRUNNELSE"),
                    row.getLocalDate("NEDSATT_ARBEIDSEVNE_DATO"),
                    row.getStringOrNull("YTTERLIGERE_NEDSATT_BEGRUNNELSE"),
                    row.getLocalDateOrNull("YTTERLIGERE_NEDSATT_ARBEIDSEVNE_DATO")
                )
            }
        }
    }

    private fun mapYrkesskadeVurdering(vurderingId: Long?): BeregningYrkeskaderBeløpVurdering? {
        if (vurderingId == null) {
            return null
        }

        val query = """
            SELECT BEGRUNNELSE, REFERANSE, ANTATT_ARLIG_INNTEKT
            FROM YRKESSKADE_INNTEKT
            WHERE INNTEKTER_ID = ?
        """.trimIndent()

        return BeregningYrkeskaderBeløpVurdering(vurderingId, connection.queryList(query) {
            setParams {
                setLong(1, vurderingId)
            }
            setRowMapper { row ->
                YrkesskadeBeløpVurdering(
                    antattÅrligInntekt = Beløp(row.getBigDecimal("ANTATT_ARLIG_INNTEKT")),
                    referanse = row.getString("REFERANSE"),
                    begrunnelse = row.getString("BEGRUNNELSE")
                )
            }
        })
    }

    fun hentHvisEksisterer(behandlingId: BehandlingId): BeregningGrunnlag? {

        val query = """
            SELECT TIDSPUNKT_VURDERING_ID,  YRKESSKADE_VURDERING_ID
            FROM BEREGNINGSFAKTA_GRUNNLAG 
            WHERE BEHANDLING_ID = ? AND AKTIV = TRUE
        """.trimIndent()
        return connection.queryFirstOrNull(query) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper { row ->
                BeregningGrunnlag(
                    tidspunktVurdering = mapTidspunktVurdering(row.getLongOrNull("TIDSPUNKT_VURDERING_ID")),
                    yrkesskadeBeløpVurdering = mapYrkesskadeVurdering(row.getLongOrNull("YRKESSKADE_VURDERING_ID"))
                )
            }
        }
    }


    fun hent(behandlingId: BehandlingId): BeregningGrunnlag {
        return requireNotNull(hentHvisEksisterer(behandlingId))
    }

    fun lagre(behandlingId: BehandlingId, vurdering: BeregningstidspunktVurdering?) {
        val eksisterendeVurdering = hentHvisEksisterer(behandlingId)

        if (eksisterendeVurdering?.tidspunktVurdering != vurdering) {
            if (eksisterendeVurdering != null) {
                deaktiverEksisterende(behandlingId)
            }

            val vurderingId = lagreVurdering(vurdering)
            val query = """
            INSERT INTO BEREGNINGSFAKTA_GRUNNLAG (BEHANDLING_ID, TIDSPUNKT_VURDERING_ID,  YRKESSKADE_VURDERING_ID) VALUES (?, ?, ?)
        """.trimIndent()

            connection.execute(query) {
                setParams {
                    setLong(1, behandlingId.toLong())
                    setLong(2, vurderingId)
                    setLong(3, eksisterendeVurdering?.yrkesskadeBeløpVurdering?.id)
                }
            }
        }
    }

    fun lagre(behandlingId: BehandlingId, vurdering: List<YrkesskadeBeløpVurdering>) {
        val eksisterendeVurdering = hentHvisEksisterer(behandlingId)

        if (eksisterendeVurdering?.yrkesskadeBeløpVurdering?.vurderinger != vurdering) {
            if (eksisterendeVurdering != null) {
                deaktiverEksisterende(behandlingId)
            }

            val vurderingId = lagreVurdering(vurdering)
            val query = """
            INSERT INTO BEREGNINGSFAKTA_GRUNNLAG (BEHANDLING_ID, TIDSPUNKT_VURDERING_ID,  YRKESSKADE_VURDERING_ID) VALUES (?, ?, ?)
        """.trimIndent()

            connection.execute(query) {
                setParams {
                    setLong(1, behandlingId.toLong())
                    setLong(2, eksisterendeVurdering?.tidspunktVurdering?.id)
                    setLong(3, vurderingId)
                }
            }
        }
    }

    private fun deaktiverEksisterende(behandlingId: BehandlingId) {
        connection.execute("UPDATE BEREGNINGSFAKTA_GRUNNLAG SET AKTIV = FALSE WHERE BEHANDLING_ID = ? AND AKTIV = TRUE") {
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
            INSERT INTO BEREGNINGSFAKTA_GRUNNLAG (BEHANDLING_ID, TIDSPUNKT_VURDERING_ID,  YRKESSKADE_VURDERING_ID) SELECT ?, TIDSPUNKT_VURDERING_ID,  YRKESSKADE_VURDERING_ID FROM BEREGNINGSFAKTA_GRUNNLAG WHERE BEHANDLING_ID = ? AND AKTIV
        """.trimIndent()

        connection.execute(query) {
            setParams {
                setLong(1, tilBehandling.toLong())
                setLong(2, fraBehandling.toLong())
            }
        }
    }

    private fun lagreVurdering(vurdering: BeregningstidspunktVurdering?): Long? {
        if (vurdering == null) {
            return null
        }

        val query = """
            INSERT INTO BEREGNINGSTIDSPUNKT_VURDERING 
            (NEDSATT_BEGRUNNELSE, NEDSATT_ARBEIDSEVNE_DATO,YTTERLIGERE_NEDSATT_ARBEIDSEVNE_DATO, YTTERLIGERE_NEDSATT_BEGRUNNELSE)
            VALUES
            (?, ?, ?, ?)
        """.trimIndent()

        val id = connection.executeReturnKey(query) {
            setParams {
                setString(1, vurdering.begrunnelse)
                setLocalDate(2, vurdering.nedsattArbeidsevneDato)
                setLocalDate(3, vurdering.ytterligereNedsattArbeidsevneDato)
                setString(4, vurdering.ytterligereNedsattBegrunnelse)
            }
        }

        return id
    }

    private fun lagreVurdering(vurderinger: List<YrkesskadeBeløpVurdering>): Long? {
        if (vurderinger.isEmpty()) {
            return null
        }

        val inntekterId = connection.executeReturnKey("INSERT INTO YRKESSKADE_INNTEKTER DEFAULT VALUES")

        val query = """
            INSERT INTO YRKESSKADE_INNTEKT
            (INNTEKTER_ID, BEGRUNNELSE, REFERANSE, ANTATT_ARLIG_INNTEKT)
            VALUES
            (?, ?, ?, ?)
        """.trimIndent()

        connection.executeBatch(query, vurderinger) {
            setParams { vurdering ->
                setLong(1, inntekterId)
                setString(2, vurdering.begrunnelse)
                setString(3, vurdering.referanse)
                setBigDecimal(4, vurdering.antattÅrligInntekt.verdi)
            }
        }

        return inntekterId
    }
}