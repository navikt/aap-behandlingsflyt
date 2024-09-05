package no.nav.aap.behandlingsflyt.behandling.underveis.sykepenger

import java.time.LocalDate

data class SykePengerRequest (
    val personidentifikatorer: Set<String>,
    val fom: LocalDate,
    val tom: LocalDate,
    val oppløsning: Set<String> = emptySet()
)

data class SykePengerResponse(
    val utbetaltePerioder: List<UtbetaltePerioder>
)

data class UtbetaltePerioder(
    val personidentifikator: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val grad: Int,
    val tags: Set<String>
)