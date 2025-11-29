package no.nav.aap.behandlingsflyt.behandling.tilbakekrevingsbehandling

import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import java.net.URI
import java.time.LocalDateTime
import java.util.UUID

data class Tilbakekrevingshendelse(
    val tilbakekrevingBehandlingId: UUID,
    val eksternFagsakId: String,
    val hendelseOpprettet: LocalDateTime,
    val eksternBehandlingId: String?,
    val sakOpprettet: LocalDateTime,
    val varselSendt: LocalDateTime?,
    val behandlingsstatus: TilbakekrevingBehandlingsstatus,
    val totaltFeilutbetaltBeløp: Beløp,
    val tilbakekrevingSaksbehandlingUrl: URI,
    val fullstendigPeriode: Periode,
    val versjon: Int,
)