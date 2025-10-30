package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway

import java.time.LocalDate

data class SykepengerResponse(
    val utbetaltePerioder: List<UtbetaltePerioder>
)

data class UtbetaltePerioder(
    val fom: LocalDate,
    val tom: LocalDate,
    val grad: Number,
    val organisasjonsnummer: String?
)