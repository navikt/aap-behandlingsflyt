package no.nav.aap.institusjonsopphold

import java.time.LocalDate

data class Soningsvurdering(
    val skalOpphøre: Boolean,
    val begrunnelse: String,
    val fraDato: LocalDate,
)