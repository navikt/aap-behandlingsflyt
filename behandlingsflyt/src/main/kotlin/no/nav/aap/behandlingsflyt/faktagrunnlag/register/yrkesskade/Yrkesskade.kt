package no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade

import java.time.LocalDate

data class Yrkesskade(
    val ref: String,
    val saksnummer: Int?,
    val kildesystem: String,
    val skadedato: LocalDate
)
