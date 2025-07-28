package no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate

import java.time.LocalDateTime
import java.util.UUID

data class BehandlingAvTypeDTO(
    val behandlingsReferanse: UUID,
    val opprettetDato: LocalDateTime
)