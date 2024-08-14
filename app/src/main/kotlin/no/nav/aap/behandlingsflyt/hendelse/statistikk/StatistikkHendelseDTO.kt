package no.nav.aap.behandlingsflyt.hendelse.statistikk

import no.nav.aap.behandlingsflyt.hendelse.avløp.AvklaringsbehovHendelseDto
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanse
import no.nav.aap.verdityper.sakogbehandling.Status
import no.nav.aap.verdityper.sakogbehandling.TypeBehandling
import java.time.LocalDateTime

data class StatistikkHendelseDTO(
    val saksnummer: String,
    val behandlingReferanse: BehandlingReferanse,
    val status: Status,
    val behandlingType: TypeBehandling,
    val ident: String,
    val avklaringsbehov: List<AvklaringsbehovHendelseDto>,
    val behandlingOpprettetTidspunkt: LocalDateTime
)