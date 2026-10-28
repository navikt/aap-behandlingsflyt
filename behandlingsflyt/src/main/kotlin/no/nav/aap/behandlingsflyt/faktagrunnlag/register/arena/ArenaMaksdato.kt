package no.nav.aap.behandlingsflyt.faktagrunnlag.register.arena

import java.time.LocalDate

data class ArenaMaksdato(
    val arenaSaksnummer: String,
    val status: String,
    val maksdato: LocalDate?,
    val vedtakTil: LocalDate?,
)
