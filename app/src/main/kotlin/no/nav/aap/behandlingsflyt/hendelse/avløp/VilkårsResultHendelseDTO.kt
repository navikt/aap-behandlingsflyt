package no.nav.aap.behandlingsflyt.hendelse.avløp

import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import java.time.LocalDateTime

data class AvsluttetBehandlingHendelseDTO(
    val behandlingId: BehandlingId,
    val hendelseTidspunkt: LocalDateTime
)

