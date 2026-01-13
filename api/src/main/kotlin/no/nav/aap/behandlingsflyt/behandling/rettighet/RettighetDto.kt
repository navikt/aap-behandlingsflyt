package no.nav.aap.behandlingsflyt.behandling.rettighet

import java.time.LocalDate

data class RettighetDto(
    val kvote: Int? = null,
    val bruktKvote: Int? = null,
    var gjenværendeKvote: Int? = null,
    val startdato: LocalDate? = null,
    val maksDato: LocalDate? = null,
    val stansdato: LocalDate? = null,
    val opphørsdato: LocalDate? = null,
)
