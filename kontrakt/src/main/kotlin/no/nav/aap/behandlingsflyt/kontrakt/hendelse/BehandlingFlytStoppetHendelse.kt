package no.nav.aap.behandlingsflyt.kontrakt.hendelse

import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import java.time.LocalDateTime

/**
 * @param status Status på behandlingen.
 * @param opprettetTidspunkt Når behandlingen ble opprettet.
 */
class BehandlingFlytStoppetHendelse(
    val personIdent: String,
    val saksnummer: Saksnummer,
    val referanse: BehandlingReferanse,
    val behandlingType: TypeBehandling,
    val status: Status,
    val avklaringsbehov: List<AvklaringsbehovHendelseDto>,
    val opprettetTidspunkt: LocalDateTime,
    val hendelsesTidspunkt: LocalDateTime,
    val versjon: String
)
