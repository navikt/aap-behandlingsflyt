package no.nav.aap.behandlingsflyt.behandling.kvote

import java.time.LocalDate

data class KvoteDto(
    val kvote: Int? = null,
    val bruktKvote: Int? = null,
    var gjenværendeKvote: Int? = null,
    val startdatoForKvote: LocalDate? = null,
    val sluttDatoForKvote: LocalDate? = null,
    val stansdato: LocalDate? = null,
    val opphørsdato: LocalDate? = null,
)
