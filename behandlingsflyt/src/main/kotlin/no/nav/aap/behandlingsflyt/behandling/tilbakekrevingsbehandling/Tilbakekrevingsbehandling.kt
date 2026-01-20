package no.nav.aap.behandlingsflyt.behandling.tilbakekrevingsbehandling

import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import java.net.URI
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class Tilbakekrevingsbehandling(
    val tilbakekrevingBehandlingId: UUID,
    val eksternFagsakId: String,
    val hendelseOpprettet: LocalDateTime,
    val eksternBehandlingId: String?,
    val sakOpprettet: LocalDateTime,
    val varselSendt: LocalDate?,
    val behandlingsstatus: TilbakekrevingBehandlingsstatus,
    val totaltFeilutbetaltBeløp: Beløp,
    val saksbehandlingURL: URI,
    val fullstendigPeriode: Periode,
)