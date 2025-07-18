package no.nav.aap.behandlingsflyt.flyt

import java.time.LocalDateTime
import java.util.UUID

data class BehandlingAvTypeDTO(
    val behandlingsReferanse: UUID,
    val opprettetDato: LocalDateTime
)