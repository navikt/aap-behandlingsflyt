package no.nav.aap.behandlingsflyt.behandling.tilbakekrevingsbehandling

import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import java.net.URI
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class Tilbakekrevingsbehandling(
    /**
     * Referanse til eksternt system.
     */
    val tilbakekrevingBehandlingId: UUID,
    /**
     * Refererer til Kelvin-saksnummer.
     */
    val eksternFagsakId: String,
    val hendelseOpprettet: LocalDateTime,
    /**
     * Referer til behandlingen i Kelvin som utløste tilbakekreving.
     */
    val eksternBehandlingId: String?,
    /**
     * Når tilbakekrevingssaken ble opprettet (ikke Kelvin-saken).
     */
    val sakOpprettet: LocalDateTime,
    val varselSendt: LocalDate?,
    val behandlingsstatus: TilbakekrevingBehandlingsstatus,
    val totaltFeilutbetaltBeløp: Beløp,
    /**
     * URL til ekstern saksbehandlingssystem.
     */
    val saksbehandlingURL: URI,
    val fullstendigPeriode: Periode,
)