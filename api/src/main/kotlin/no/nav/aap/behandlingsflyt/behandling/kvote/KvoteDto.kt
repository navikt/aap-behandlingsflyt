package no.nav.aap.behandlingsflyt.behandling.kvote

import java.time.LocalDate

data class KvoteDto(
    val kvote: Int? = null,
    val bruktKvote: Int? = null,
    var gjenværendeKvote: Int? = null,
    val senesteDatoForKvote: LocalDate? = null,
    val stansdato: LocalDate? = null,
    val opphørsdato: LocalDate? = null,
    val rettighetStartDato: LocalDate? = null,
    val rettighetEndDato: LocalDate? = null
)
