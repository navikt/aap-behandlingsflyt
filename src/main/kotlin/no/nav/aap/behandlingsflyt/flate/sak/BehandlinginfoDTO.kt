package no.nav.aap.behandlingsflyt.flate.sak

import no.nav.aap.behandlingsflyt.domene.behandling.Status
import java.time.LocalDateTime
import java.util.*

data class BehandlinginfoDTO(
    val referanse: UUID,
    val type: String,
    val status: Status,
    val opprettet: LocalDateTime
) {

}
