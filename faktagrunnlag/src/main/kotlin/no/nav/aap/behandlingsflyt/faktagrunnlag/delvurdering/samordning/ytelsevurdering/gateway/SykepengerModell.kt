package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway

import java.time.LocalDate

//personidentifikatorer er for en liste om vedkommende har hatt flere personidentifikatorerer.
//Ikke for å slå opp fler personer i samme oppslag. Da blir responsen bare krøll - Team SP

data class SykepengerRequest (
    val personidentifikatorer: Set<String>,
    val fom: LocalDate,
    val tom: LocalDate
)

data class SykepengerResponse(
    val utbetaltePerioder: List<UtbetaltePerioder>
)

data class UtbetaltePerioder(
    val fom: LocalDate,
    val tom: LocalDate,
    val grad: Number,
)