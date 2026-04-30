package no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade

import java.time.LocalDate

data class Yrkesskade(
    val ref: String,
    val saksnummer: Int?,
    val kildesystem: String,
    val skadedato: LocalDate?,
    val vedtaksdato: LocalDate? = null,
    val skadeart: String? = null,
    val diagnose: String? = null,
    val skadekombinasjoner: List<SkadekombinasjonRegister>? = null,
    val skadekombinasjonerTekst: String? = null,
)

data class SkadekombinasjonRegister(
    val kroppsdel: String,
    val skadetype: String,
)
