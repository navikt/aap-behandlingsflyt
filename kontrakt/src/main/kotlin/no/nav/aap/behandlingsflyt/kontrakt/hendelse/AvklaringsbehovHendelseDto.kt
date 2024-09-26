package no.nav.aap.behandlingsflyt.kontrakt.hendelse

import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import java.time.LocalDate
import java.time.LocalDateTime

data class AvklaringsbehovHendelseDto(
    val definisjon: DefinisjonDTO,
    val status: Status,
    val endringer: List<EndringDTO>
)

data class DefinisjonDTO(
    val type: String,
    val behovType: Definisjon.BehovType,
    val løsesISteg: StegType
)

data class EndringDTO(
    val status: Status,
    val tidsstempel: LocalDateTime = LocalDateTime.now(),
    val frist: LocalDate? = null,
    val endretAv: String
)