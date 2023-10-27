package no.nav.aap.behandlingsflyt.sak.flate

import no.nav.aap.behandlingsflyt.behandling.Status
import java.time.LocalDateTime
import java.util.*

data class BehandlinginfoDTO(
    val referanse: UUID,
    val type: String,
    val status: Status,
    val opprettet: LocalDateTime
)
