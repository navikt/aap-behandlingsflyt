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
 * @param reserverTil Hvis satt, så forventes det at oppgaveappen reserverer oppgaven til denne hendelsen til denne identen.
 */
public data class BehandlingFlytStoppetHendelse(
    val personIdent: String,
    val saksnummer: Saksnummer,
    val referanse: BehandlingReferanse,
    val behandlingType: TypeBehandling,
    @Deprecated("Kan fjernes når oppgave har byttet til å bruke vurderingsbehov")
    val årsakerTilBehandling: List<String>,
    val vurderingsbehov: List<String>,
    val årsakTilOpprettelse: String,
    val status: Status,
    val aktivtSteg: StegType? = null,
    val avklaringsbehov: List<AvklaringsbehovHendelseDto>,
    val erPåVent: Boolean,
    val relevanteIdenterPåBehandling: List<String> = emptyList(),
    val mottattDokumenter: List<MottattDokumentDto>,
    val reserverTil: String? = null,
    val opprettetTidspunkt: LocalDateTime,
    val hendelsesTidspunkt: LocalDateTime,
    val versjon: String
)
