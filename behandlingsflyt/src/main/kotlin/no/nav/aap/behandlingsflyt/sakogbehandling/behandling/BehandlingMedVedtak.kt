package no.nav.aap.behandlingsflyt.sakogbehandling.behandling

import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import java.time.LocalDate
import java.time.LocalDateTime

data class BehandlingMedVedtak(
    val id: BehandlingId,
    val referanse: BehandlingReferanse,
    val typeBehandling: TypeBehandling,
    val status: Status,
    val opprettetTidspunkt: LocalDateTime,
    val vedtakstidspunkt: LocalDateTime,
    val virkningstidspunkt: LocalDate?,
)