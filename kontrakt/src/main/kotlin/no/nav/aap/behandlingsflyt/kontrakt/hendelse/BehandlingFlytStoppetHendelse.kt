package no.nav.aap.behandlingsflyt.kontrakt.hendelse

import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import java.time.LocalDateTime

/**
 * @param status Status på behandlingen.
 * @param opprettetTidspunkt Når behandlingen ble opprettet.
 * @param hendelsesTidspunkt Når denne hendelsen ble opprettet i Behandlingsflyt.
 */
public data class BehandlingFlytStoppetHendelse(
    val personIdent: String,
    val saksnummer: Saksnummer,
    val referanse: BehandlingReferanse,
    val behandlingType: TypeBehandling,
    // TODO: fjern default emptyList her
    val årsakerTilBehandling: List<String> = emptyList(),
    val status: Status,
    val aktivtSteg: StegType? = null,
    val avklaringsbehov: List<AvklaringsbehovHendelseDto>,
    val erPåVent: Boolean,
    val opprettetTidspunkt: LocalDateTime,
    val hendelsesTidspunkt: LocalDateTime,
    val versjon: String
)
