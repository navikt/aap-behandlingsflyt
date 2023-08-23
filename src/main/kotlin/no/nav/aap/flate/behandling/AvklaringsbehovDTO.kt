package no.nav.aap.flate.behandling

import no.nav.aap.domene.behandling.avklaringsbehov.Definisjon
import no.nav.aap.domene.behandling.avklaringsbehov.Status
import java.time.LocalDateTime

data class AvklaringsbehovDTO(
    val definisjon: Definisjon,
    val status: Status,
    val endringer: List<EndringDTO>
)

data class EndringDTO(
    val status: Status,
    val tidsstempel: LocalDateTime = LocalDateTime.now(),
    val begrunnelse: String,
    val endretAv: String
)
