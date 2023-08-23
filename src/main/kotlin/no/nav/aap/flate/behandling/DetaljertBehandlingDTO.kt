package no.nav.aap.flate.behandling

import no.nav.aap.domene.behandling.Status
import no.nav.aap.flyt.StegType
import java.time.LocalDateTime
import java.util.*

data class DetaljertBehandlingDTO(
    val referanse: UUID,
    val type: String,
    val status: Status,
    val opprettet: LocalDateTime,
    val avklaringsbehov: List<AvklaringsbehovDTO>,
    val aktivtSteg: StegType
)
