package no.nav.aap.behandlingsflyt.kontrakt.hendelse

import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.TilbakekrevingBehandlingsstatus
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.TilbakekrevingVenteGrunn
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

public data class TilbakekrevingsbehandlingOppdatertHendelse(
    val personIdent: String,
    val saksnummer: Saksnummer,
    /**
     * Refererer til en ekstern behandlingsreferanse, til tross for at BehandlingReferanse-typen er brukt her.
     */
    val behandlingref: BehandlingReferanse,
    val behandlingStatus: TilbakekrevingBehandlingsstatus,
    val sakOpprettet: LocalDateTime,
    val totaltFeilutbetaltBeløp: BigDecimal,
    val saksbehandlingURL: String,
    val gjenopptas: LocalDate? = null,
    val venteGrunn: TilbakekrevingVenteGrunn? = null,
)
