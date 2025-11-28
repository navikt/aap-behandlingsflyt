package no.nav.aap.behandlingsflyt.repository.behandling.tilbakekrevingsbehandling

import no.nav.aap.behandlingsflyt.behandling.tilbakekrevingsbehandling.Tilbakekrevingsbehandling
import no.nav.aap.behandlingsflyt.behandling.tilbakekrevingsbehandling.TilbakekrevingRepository
import no.nav.aap.behandlingsflyt.behandling.tilbakekrevingsbehandling.Tilbakekrevingshendelse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.lookup.repository.Factory
import java.net.URI

class TilbakekrevingRepositoryImpl(private val connection: DBConnection) : TilbakekrevingRepository {

    override fun lagre(sakId: SakId, tilbakekrevingshendelse: Tilbakekrevingshendelse) {
        val insertHendelse = """
            INSERT INTO TILBAKEKREVINGSHENDELSE(
                SAK_ID,
                TILBAKEKREVING_BEHANDLING_ID,
                EKSTERN_FAGSAK_ID,
                HENDELSE_OPPRETTET,
                EKSTERN_BEHANDLING_ID,
                SAK_OPPRETTET,
                VARSEL_SENDT,
                BEHANDLINGSSTATUS,
                TOTALT_FEILUTBETALT_BELOP,
                TILBAKEKREVING_SAKSBEHANDLING_URL,
                FULLSTENDIG_PERIODE,
                VERSJON
            ) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::daterange, ?)
        """.trimIndent()

        connection.execute(insertHendelse) {
            setParams {
                setLong(1, sakId.id)
                setUUID(2, tilbakekrevingshendelse.tilbakekrevingBehandlingId)
                setString(3, tilbakekrevingshendelse.eksternFagsakId)
                setLocalDateTime(4, tilbakekrevingshendelse.hendelseOpprettet)
                setString(5, tilbakekrevingshendelse.eksternBehandlingId)
                setLocalDateTime(6, tilbakekrevingshendelse.sakOpprettet)
                setLocalDateTime(7, tilbakekrevingshendelse.varselSendt)
                setEnumName(8, tilbakekrevingshendelse.behandlingsstatus)
                setBigDecimal(9, tilbakekrevingshendelse.totaltFeilutbetaltBeløp.verdi)
                setString(10, tilbakekrevingshendelse.tilbakekrevingSaksbehandlingUrl.toString())
                setPeriode(11, tilbakekrevingshendelse.fullstendigPeriode)
                setInt(12, tilbakekrevingshendelse.versjon)
            }
        }

        lagreEllerOppdatereBehandling(sakId, tilbakekrevingshendelse)
    }

    private fun lagreEllerOppdatereBehandling(sakId: SakId, tilbakekrevingshendelse: Tilbakekrevingshendelse) {
        val upsertBehandling = """
            INSERT INTO TILBAKEKREVINGSBEHANDLING(
                SAK_ID,
                TILBAKEKREVING_BEHANDLING_ID,
                EKSTERN_FAGSAK_ID,
                HENDELSE_OPPRETTET,
                EKSTERN_BEHANDLING_ID,
                SAK_OPPRETTET,
                VARSEL_SENDT,
                BEHANDLINGSSTATUS,
                TOTALT_FEILUTBETALT_BELOP,
                TILBAKEKREVING_SAKSBEHANDLING_URL,
                FULLSTENDIG_PERIODE
            ) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::daterange)
            ON CONFLICT(TILBAKEKREVING_BEHANDLING_ID) DO UPDATE SET 
                HENDELSE_OPPRETTET = EXCLUDED.HENDELSE_OPPRETTET,
                VARSEL_SENDT = EXCLUDED.VARSEL_SENDT,
                BEHANDLINGSSTATUS = EXCLUDED.BEHANDLINGSSTATUS,
                TOTALT_FEILUTBETALT_BELOP = EXCLUDED.TOTALT_FEILUTBETALT_BELOP,
                TILBAKEKREVING_SAKSBEHANDLING_URL = EXCLUDED.TILBAKEKREVING_SAKSBEHANDLING_URL,
                FULLSTENDIG_PERIODE = EXCLUDED.FULLSTENDIG_PERIODE
        """.trimIndent()

        connection.execute(upsertBehandling) {
            setParams {
                setLong(1, sakId.id)
                setUUID(2, tilbakekrevingshendelse.tilbakekrevingBehandlingId)
                setString(3, tilbakekrevingshendelse.eksternFagsakId)
                setLocalDateTime(4, tilbakekrevingshendelse.hendelseOpprettet)
                setString(5, tilbakekrevingshendelse.eksternBehandlingId)
                setLocalDateTime(6, tilbakekrevingshendelse.sakOpprettet)
                setLocalDateTime(7, tilbakekrevingshendelse.varselSendt)
                setEnumName(8, tilbakekrevingshendelse.behandlingsstatus)
                setBigDecimal(9, tilbakekrevingshendelse.totaltFeilutbetaltBeløp.verdi)
                setString(10, tilbakekrevingshendelse.tilbakekrevingSaksbehandlingUrl.toString())
                setPeriode(11, tilbakekrevingshendelse.fullstendigPeriode)
            }
        }
    }

    override fun hent(sakId: SakId): List<Tilbakekrevingsbehandling> {
        val sql = """
            SELECT
                EKSTERN_FAGSAK_ID,
                HENDELSE_OPPRETTET,
                EKSTERN_BEHANDLING_ID,
                SAK_OPPRETTET,
                VARSEL_SENDT,
                BEHANDLINGSSTATUS,
                TOTALT_FEILUTBETALT_BELOP,
                TILBAKEKREVING_SAKSBEHANDLING_URL,
                FULLSTENDIG_PERIODE
            FROM TILBAKEKREVINGSBEHANDLING
            WHERE SAK_ID = ?
        """.trimIndent()

        return connection.queryList(sql) {
            setParams {
                setLong(1, sakId.id)
            }
            setRowMapper { row ->
                Tilbakekrevingsbehandling(
                    eksternFagsakId = row.getString("EKSTERN_FAGSAK_ID") ,
                    hendelseOpprettet = row.getLocalDateTime("HENDELSE_OPPRETTET"),
                    eksternBehandlingId = row.getStringOrNull("EKSTERN_BEHANDLING_ID"),
                    sakOpprettet = row.getLocalDateTime("SAK_OPPRETTET"),
                    varselSendt = row.getLocalDateTimeOrNull("VARSEL_SENDT"),
                    behandlingsstatus = row.getEnum("BEHANDLINGSSTATUS"),
                    totaltFeilutbetaltBeløp = Beløp(row.getBigDecimal("TOTALT_FEILUTBETALT_BELOP")),
                    saksbehandlingURL = URI.create(row.getString("TILBAKEKREVING_SAKSBEHANDLING_URL")),
                    fullstendigPeriode = row.getPeriode("FULLSTENDIG_PERIODE")
                )
            }

        }
    }

    override fun kopier(
        fraBehandling: BehandlingId,
        tilBehandling: BehandlingId
    ) {
        // Ikke nødvendig siden disse tilbakekrevingene ikke er koblet på behandling men på sak.
    }

    override fun slett(behandlingId: BehandlingId) {
        // Ikke nødvendig siden disse tilbakekrevingene ikke er koblet på behandling men på sak.
    }

    companion object : Factory<TilbakekrevingRepositoryImpl> {
        override fun konstruer(connection: DBConnection): TilbakekrevingRepositoryImpl {
            return TilbakekrevingRepositoryImpl(connection)
        }
    }

}