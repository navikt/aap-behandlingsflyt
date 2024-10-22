package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon

import java.time.LocalDate

data class Soningsvurdering(
    val skalOpphøre: Boolean,
    val begrunnelse: String,
    val fraDato: LocalDate,
)
