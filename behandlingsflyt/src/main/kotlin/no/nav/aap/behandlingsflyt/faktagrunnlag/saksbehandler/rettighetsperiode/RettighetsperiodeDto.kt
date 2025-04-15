package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.rettighetsperiode

import java.time.LocalDate

data class RettighetsperiodeDto(
    val startDato: LocalDate,
    val begrunnelse: String,
)