package no.nav.aap.behandlingsflyt.behandling.flate

import no.nav.aap.behandlingsflyt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.avklaringsbehov.Status
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
