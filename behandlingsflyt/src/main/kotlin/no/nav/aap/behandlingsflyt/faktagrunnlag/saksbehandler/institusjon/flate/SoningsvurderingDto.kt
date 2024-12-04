package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.flate

import java.time.LocalDate

data class SoningsvurderingDto(
    val skalOpphore: Boolean,
    val begrunnelse: String,
    val fraDato: LocalDate
)
