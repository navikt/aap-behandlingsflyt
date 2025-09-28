package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.beregning

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningVurderingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningYrkeskaderBeløpVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningstidspunktVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.YrkesskadeBeløpVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.lookup.repository.Factory
import org.slf4j.LoggerFactory
import java.math.BigDecimal
import java.math.RoundingMode

class BeregningVurderingRepositoryImpl(private val connection: DBConnection) : BeregningVurderingRepository {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object : Factory<BeregningVurderingRepositoryImpl> {
        override fun konstruer(connection: DBConnection): BeregningVurderingRepositoryImpl {
            return BeregningVurderingRepositoryImpl(connection)
        }
    }

    private fun mapTidspunktVurdering(vurderingId: Long?): BeregningstidspunktVurdering? {
        if (vurderingId == null) {
            return null
        }
        val query = """
            SELECT NEDSATT_BEGRUNNELSE, NEDSATT_ARBEIDSEVNE_DATO, YTTERLIGERE_NEDSATT_ARBEIDSEVNE_DATO, YTTERLIGERE_NEDSATT_BEGRUNNELSE, VURDERT_AV, OPPRETTET_TID
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
                    row.getLocalDateOrNull("YTTERLIGERE_NEDSATT_ARBEIDSEVNE_DATO"),
                    row.getString("VURDERT_AV"),
                    row.getLocalDateTime("OPPRETTET_TID"),
                )
            }
        }
    }

    private fun mapYrkesskadeVurdering(vurderingId: Long?): BeregningYrkeskaderBeløpVurdering? {
        if (vurderingId == null) {
            return null
        }

        val query = """
            SELECT BEGRUNNELSE, REFERANSE, ANTATT_ARLIG_INNTEKT, VURDERT_AV, OPPRETTET_TID
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
                    begrunnelse = row.getString("BEGRUNNELSE"),
                    vurdertAv = row.getString("VURDERT_AV"),
                    vurdertTidspunkt = row.getLocalDateTime("OPPRETTET_TID")
                )
            }
        }
        )
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): BeregningGrunnlag? {

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

    override fun hentHistoriskeVurderinger(
        sakId: SakId,
        behandlingId: BehandlingId,
        ekskluderteBehandlingIdListe: List<BehandlingId>
    ): List<BeregningGrunnlag> {
        val harEkskludering = ekskluderteBehandlingIdListe.isNotEmpty()
        var query = """
            SELECT TIDSPUNKT_VURDERING_ID,  YRKESSKADE_VURDERING_ID
            FROM BEREGNINGSFAKTA_GRUNNLAG BG
                     JOIN BEHANDLING B ON BG.BEHANDLING_ID = B.ID
            WHERE BG.AKTIV
              AND B.SAK_ID = ?
              AND B.OPPRETTET_TID < (SELECT A.OPPRETTET_TID FROM BEHANDLING A WHERE ID = ?)
        """.trimIndent()

        if (harEkskludering) {
            query = "$query AND B.ID <> ALL(?::bigint[])"
        }

        return connection.queryList(query) {
            setParams {
                setLong(1, sakId.id)
                setLong(2, behandlingId.id)
                if (harEkskludering) {
                    setLongArray(3, ekskluderteBehandlingIdListe.map { it.toLong() })
                }
            }
            setRowMapper { row ->
                BeregningGrunnlag(
                    tidspunktVurdering = mapTidspunktVurdering(row.getLongOrNull("TIDSPUNKT_VURDERING_ID")),
                    yrkesskadeBeløpVurdering = mapYrkesskadeVurdering(row.getLongOrNull("YRKESSKADE_VURDERING_ID"))
                )
            }
        }
    }


    override fun hent(behandlingId: BehandlingId): BeregningGrunnlag {
        return requireNotNull(hentHvisEksisterer(behandlingId))
    }

    override fun lagre(behandlingId: BehandlingId, vurdering: BeregningstidspunktVurdering?) {
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

    override fun lagre(behandlingId: BehandlingId, vurdering: List<YrkesskadeBeløpVurdering>) {
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

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        val eksisterendeGrunnlag = hentHvisEksisterer(fraBehandling)
        if (eksisterendeGrunnlag == null) {
            log.info("Fant ikke eksisterende grunnlag for behandling $fraBehandling, kan derfor ikke kopiere til behandling $tilBehandling.")
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

    override fun slett(behandlingId: BehandlingId) {
        val beregningTidspunktVurderingIds = getBeregningTidspunktVurderingIds(behandlingId)
        val beregningYrkesskadeIds = getBeregningYrkesskadeIds(behandlingId)
        val yrkesskadeInntekterIds = getYrkesskadeInntekterIds(beregningYrkesskadeIds)

        val deletedRows = connection.executeReturnUpdated(
            """
            delete from BEREGNINGSFAKTA_GRUNNLAG where behandling_id = ?; 
            delete from BEREGNINGSTIDSPUNKT_VURDERING where id = ANY(?::bigint[]);
            delete from YRKESSKADE_INNTEKT where inntekter_id = ANY(?::bigint[]);
            delete from YRKESSKADE_INNTEKTER where id = ANY(?::bigint[]);

        """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.id)
                setLongArray(2, beregningTidspunktVurderingIds)
                setLongArray(3, yrkesskadeInntekterIds)
                setLongArray(4, beregningYrkesskadeIds)
            }
        }
        log.info("Slettet $deletedRows rader fra BEREGNINGSFAKTA_GRUNNLAG")
    }

    private fun getBeregningYrkesskadeIds(behandlingId: BehandlingId): List<Long> = connection.queryList(
        """
                    SELECT yrkesskade_vurdering_id
                    FROM BEREGNINGSFAKTA_GRUNNLAG
                    WHERE behandling_id = ? AND yrkesskade_vurdering_id is not null

                """.trimIndent()
    ) {
        setParams { setLong(1, behandlingId.id) }
        setRowMapper { row ->
            row.getLong("yrkesskade_vurdering_id")
        }
    }

    private fun getBeregningTidspunktVurderingIds(behandlingId: BehandlingId): List<Long> = connection.queryList(
        """
                    SELECT tidspunkt_vurdering_id
                    FROM BEREGNINGSFAKTA_GRUNNLAG
                    WHERE behandling_id = ? AND tidspunkt_vurdering_id is not null
                 
                """.trimIndent()
    ) {
        setParams { setLong(1, behandlingId.id) }
        setRowMapper { row ->
            row.getLong("tidspunkt_vurdering_id")
        }
    }

    private fun getYrkesskadeInntekterIds(yrkeskadeInntekterIds: List<Long>): List<Long> = connection.queryList(
        """
                    SELECT id
                    FROM YRKESSKADE_INNTEKTER
                    WHERE id = ANY(?::bigint[]);
                 
                """.trimIndent()
    ) {
        setParams { setLongArray(1, yrkeskadeInntekterIds) }
        setRowMapper { row ->
            row.getLong("id")
        }
    }

    private fun lagreVurdering(vurdering: BeregningstidspunktVurdering?): Long? {
        if (vurdering == null) {
            return null
        }

        val query = """
            INSERT INTO BEREGNINGSTIDSPUNKT_VURDERING 
            (NEDSATT_BEGRUNNELSE, NEDSATT_ARBEIDSEVNE_DATO,YTTERLIGERE_NEDSATT_ARBEIDSEVNE_DATO, YTTERLIGERE_NEDSATT_BEGRUNNELSE, VURDERT_AV)
            VALUES
            (?, ?, ?, ?, ?)
        """.trimIndent()

        val id = connection.executeReturnKey(query) {
            setParams {
                setString(1, vurdering.begrunnelse)
                setLocalDate(2, vurdering.nedsattArbeidsevneDato)
                setLocalDate(3, vurdering.ytterligereNedsattArbeidsevneDato)
                setString(4, vurdering.ytterligereNedsattBegrunnelse)
                setString(5, vurdering.vurdertAv)
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
            (INNTEKTER_ID, BEGRUNNELSE, REFERANSE, ANTATT_ARLIG_INNTEKT, VURDERT_AV)
            VALUES
            (?, ?, ?, ?, ?)
        """.trimIndent()

        connection.executeBatch(query, vurderinger) {
            setParams { vurdering ->
                setLong(1, inntekterId)
                setString(2, vurdering.begrunnelse)
                setString(3, vurdering.referanse)
                setBigDecimal(4, vurdering.antattÅrligInntekt.avrundOppTilNærmesteTusen())
                setString(5, vurdering.vurdertAv)
            }
        }

        return inntekterId
    }

    private fun Beløp.avrundOppTilNærmesteTusen(): BigDecimal {
        val tusen = BigDecimal(1000)
        return if (verdi.remainder(tusen) == BigDecimal.ZERO) {
            verdi
        } else {
            verdi.divide(tusen, 0, RoundingMode.UP).multiply(tusen)
        }
    }

}
