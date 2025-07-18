package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import java.time.LocalDateTime

data class BehandlingAvTypeDTO(
    val behandlingsReferanse: BehandlingReferanse,
    val opprettetDato: LocalDateTime
)