package no.nav.aap.behandlingsflyt.behandling.tilbakekrevingsbehandling

import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import java.net.URI
import java.time.LocalDateTime

data class Tilbakekrevingshendelse(
    val eksternFagsakId: String,
    val hendelseOpprettet: LocalDateTime,
    val eksternBehandlingId: String?,
    val sakOpprettet: LocalDateTime,
    val varselSendt: LocalDateTime?,
    val behandlingsstatus: TilbakekrevingBehandlingsstatus,
    val totaltFeilutbetaltBeløp: Beløp,
    val saksbehandlingURL: URI,
    val fullstendigPeriode: Periode,
    val versjon: Int,
)