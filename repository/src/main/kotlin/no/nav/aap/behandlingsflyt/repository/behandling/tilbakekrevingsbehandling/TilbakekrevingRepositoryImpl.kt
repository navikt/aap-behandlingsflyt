package no.nav.aap.behandlingsflyt.repository.behandling.tilbakekrevingsbehandling

import no.nav.aap.behandlingsflyt.behandling.tilbakekrevingsbehandling.TilbakekrevingRepository
import no.nav.aap.behandlingsflyt.behandling.tilbakekrevingsbehandling.Tilbakekrevingsbehandling
import no.nav.aap.behandlingsflyt.behandling.tilbakekrevingsbehandling.Tilbakekrevingshendelse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.lookup.repository.Factory
import java.net.URI
import java.util.*

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
                VENTE_GRUNN,
                GJENOPPTAS,
                BEHANDLINGSSTATUS,
                TOTALT_FEILUTBETALT_BELOP,
                TILBAKEKREVING_SAKSBEHANDLING_URL,
                FULLSTENDIG_PERIODE,
                VERSJON,
                VEDTAKSDATO
            ) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::daterange, ?, ?)
        """.trimIndent()

        connection.execute(insertHendelse) {
            setParams {
                setLong(1, sakId.id)
                setUUID(2, tilbakekrevingshendelse.tilbakekrevingBehandlingId)
                setString(3, tilbakekrevingshendelse.eksternFagsakId)
                setLocalDateTime(4, tilbakekrevingshendelse.hendelseOpprettet)
                setString(5, tilbakekrevingshendelse.eksternBehandlingId)
                setLocalDateTime(6, tilbakekrevingshendelse.sakOpprettet)
                setLocalDate(7, tilbakekrevingshendelse.varselSendt)
                setEnumName(8, tilbakekrevingshendelse.venteGrunn)
                setLocalDate(9, tilbakekrevingshendelse.gjenopptas)
                setEnumName(10, tilbakekrevingshendelse.behandlingsstatus)
                setBigDecimal(11, tilbakekrevingshendelse.totaltFeilutbetaltBeløp.verdi)
                setString(12, tilbakekrevingshendelse.tilbakekrevingSaksbehandlingUrl.toString())
                setPeriode(13, tilbakekrevingshendelse.fullstendigPeriode)
                setInt(14, tilbakekrevingshendelse.versjon)
                setLocalDate(15, tilbakekrevingshendelse.vedtaksdato)
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
                VENTE_GRUNN,
                GJENOPPTAS,
                BEHANDLINGSSTATUS,
                TOTALT_FEILUTBETALT_BELOP,
                TILBAKEKREVING_SAKSBEHANDLING_URL,
                FULLSTENDIG_PERIODE,
                VEDTAKSDATO
            ) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::daterange, ?)
            ON CONFLICT(TILBAKEKREVING_BEHANDLING_ID) DO UPDATE SET 
                HENDELSE_OPPRETTET = EXCLUDED.HENDELSE_OPPRETTET,
                EKSTERN_BEHANDLING_ID = EXCLUDED.EKSTERN_BEHANDLING_ID,
                VARSEL_SENDT = EXCLUDED.VARSEL_SENDT,
                VENTE_GRUNN = EXCLUDED.VENTE_GRUNN,
                GJENOPPTAS = EXCLUDED.GJENOPPTAS,
                BEHANDLINGSSTATUS = EXCLUDED.BEHANDLINGSSTATUS,
                TOTALT_FEILUTBETALT_BELOP = EXCLUDED.TOTALT_FEILUTBETALT_BELOP,
                TILBAKEKREVING_SAKSBEHANDLING_URL = EXCLUDED.TILBAKEKREVING_SAKSBEHANDLING_URL,
                FULLSTENDIG_PERIODE = EXCLUDED.FULLSTENDIG_PERIODE,
                VEDTAKSDATO = EXCLUDED.VEDTAKSDATO
        """.trimIndent()

        connection.execute(upsertBehandling) {
            setParams {
                setLong(1, sakId.id)
                setUUID(2, tilbakekrevingshendelse.tilbakekrevingBehandlingId)
                setString(3, tilbakekrevingshendelse.eksternFagsakId)
                setLocalDateTime(4, tilbakekrevingshendelse.hendelseOpprettet)
                setString(5, tilbakekrevingshendelse.eksternBehandlingId)
                setLocalDateTime(6, tilbakekrevingshendelse.sakOpprettet)
                setLocalDate(7, tilbakekrevingshendelse.varselSendt)
                setEnumName(8, tilbakekrevingshendelse.venteGrunn)
                setLocalDate(9, tilbakekrevingshendelse.gjenopptas)
                setEnumName(10, tilbakekrevingshendelse.behandlingsstatus)
                setBigDecimal(11, tilbakekrevingshendelse.totaltFeilutbetaltBeløp.verdi)
                setString(12, tilbakekrevingshendelse.tilbakekrevingSaksbehandlingUrl.toString())
                setPeriode(13, tilbakekrevingshendelse.fullstendigPeriode)
                setLocalDate(14, tilbakekrevingshendelse.vedtaksdato)
            }
        }
    }

    override fun hent(sakId: SakId): List<Tilbakekrevingsbehandling> {
        val sql = """
            SELECT
                TILBAKEKREVING_BEHANDLING_ID,
                EKSTERN_FAGSAK_ID,
                HENDELSE_OPPRETTET,
                EKSTERN_BEHANDLING_ID,
                SAK_OPPRETTET,
                VARSEL_SENDT,
                VENTE_GRUNN,
                GJENOPPTAS,
                BEHANDLINGSSTATUS,
                TOTALT_FEILUTBETALT_BELOP,
                TILBAKEKREVING_SAKSBEHANDLING_URL,
                FULLSTENDIG_PERIODE,
                VEDTAKSDATO
            FROM TILBAKEKREVINGSBEHANDLING
            WHERE SAK_ID = ? AND AKTIV = TRUE
        """.trimIndent()

        return connection.queryList(sql) {
            setParams {
                setLong(1, sakId.id)
            }
            setRowMapper { mapToTilbakekrevingsbehandling(it) }
        }
    }

    override fun hent(tilbakekrevingsBehandlingId: UUID): Tilbakekrevingsbehandling {
        val sql = """
            SELECT
                TILBAKEKREVING_BEHANDLING_ID,
                EKSTERN_FAGSAK_ID,
                HENDELSE_OPPRETTET,
                EKSTERN_BEHANDLING_ID,
                SAK_OPPRETTET,
                VARSEL_SENDT,
                VENTE_GRUNN,
                GJENOPPTAS,
                BEHANDLINGSSTATUS,
                TOTALT_FEILUTBETALT_BELOP,
                TILBAKEKREVING_SAKSBEHANDLING_URL,
                FULLSTENDIG_PERIODE,
                VEDTAKSDATO
            FROM TILBAKEKREVINGSBEHANDLING
            WHERE TILBAKEKREVING_BEHANDLING_ID = ? AND AKTIV = TRUE
        """.trimIndent()

        return connection.queryFirst(sql) {
            setParams {
                setUUID(1, tilbakekrevingsBehandlingId)
            }
            setRowMapper { mapToTilbakekrevingsbehandling(it) }
        }
    }

    /** Vedtaksdato i kafka-hendelser fra tilbake-løsningen til kelvin ble innført i 2026. Dvs. vi har persistert
     *  tilbakekrevingsbehandlinger fra tilbake i kelvin-db som er både uten og med vedtaksdato avhengig av
     *  opprettelsetidspunkt. For visningen av vedtaksdato i klage-flyten i saksbehandling faller vi tilbake til
     *  hendelse_opprettet dato for de behandlingene som mangler vedtaksdato.
     *
     *  Filtrerer på AKTIV = TRUE for å være konsistent med hent(UUID), som kun finner aktive behandlinger.
     *  Uten dette kunne en logisk slettet (arkivert) tilbakekrevingsbehandling dukke opp som valgbar i
     *  klage-flyten, men feile når den senere skal hentes via hent(UUID).
     *
     *  TODO: Men skal vi kunne lage på tilbakekrevingssaker hvor aktiv = FALSE ?
     */
    override fun hentAvsluttaTilbakekrevingsBehandlinger(sakId: SakId): List<Tilbakekrevingsbehandling> {
        val sql = """
            SELECT
                TB.TILBAKEKREVING_BEHANDLING_ID,
                TB.EKSTERN_FAGSAK_ID,
                TB.HENDELSE_OPPRETTET,
                TB.EKSTERN_BEHANDLING_ID,
                TB.SAK_OPPRETTET,
                TB.VARSEL_SENDT,
                TB.VENTE_GRUNN,
                TB.GJENOPPTAS,
                TB.BEHANDLINGSSTATUS,
                TB.TOTALT_FEILUTBETALT_BELOP,
                TB.TILBAKEKREVING_SAKSBEHANDLING_URL,
                TB.FULLSTENDIG_PERIODE,
                TB.VEDTAKSDATO
            FROM 
                TILBAKEKREVINGSBEHANDLING TB 
            WHERE 
                TB.SAK_ID = ? AND
                TB.BEHANDLINGSSTATUS = 'AVSLUTTET' AND
                TB.AKTIV = TRUE
            ORDER BY
                TB.SAK_OPPRETTET DESC
        """.trimIndent()

        return connection.queryList(sql) {
            setParams {
                setLong(1, sakId.id)
            }
            setRowMapper { mapToTilbakekrevingsbehandling(it) }
        }
    }

    private fun mapToTilbakekrevingsbehandling(row: Row) =
        Tilbakekrevingsbehandling(
            tilbakekrevingBehandlingId = row.getUUID("TILBAKEKREVING_BEHANDLING_ID"),
            eksternFagsakId = row.getString("EKSTERN_FAGSAK_ID"),
            hendelseOpprettet = row.getLocalDateTime("HENDELSE_OPPRETTET"),
            eksternBehandlingId = row.getStringOrNull("EKSTERN_BEHANDLING_ID"),
            sakOpprettet = row.getLocalDateTime("SAK_OPPRETTET"),
            varselSendt = row.getLocalDateOrNull("VARSEL_SENDT"),
            venteGrunn = row.getEnumOrNull("VENTE_GRUNN"),
            gjenopptas = row.getLocalDateOrNull("GJENOPPTAS"),
            behandlingsstatus = row.getEnum("BEHANDLINGSSTATUS"),
            totaltFeilutbetaltBeløp = Beløp(row.getBigDecimal("TOTALT_FEILUTBETALT_BELOP")),
            saksbehandlingURL = URI.create(row.getString("TILBAKEKREVING_SAKSBEHANDLING_URL")),
            fullstendigPeriode = row.getPeriode("FULLSTENDIG_PERIODE"),
            vedtaksdato = row.getLocalDateOrNull("VEDTAKSDATO")
        )

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