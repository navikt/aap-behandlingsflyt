package no.nav.aap.behandlingsflyt.kontrakt.hendelse

import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.TilbakekrevingBehandlingsstatus
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import java.time.LocalDateTime

public data class TilbakekrevingsbehandlingOppdatertHendelse(
    val personIdent: String,
    val saksnummer: Saksnummer,
    val behandlingref: BehandlingReferanse,
    val behandlingStatus: TilbakekrevingBehandlingsstatus,
    val sakOpprettet: LocalDateTime,
    val avklaringsbehov: List<AvklaringsbehovHendelseDto>,
)
