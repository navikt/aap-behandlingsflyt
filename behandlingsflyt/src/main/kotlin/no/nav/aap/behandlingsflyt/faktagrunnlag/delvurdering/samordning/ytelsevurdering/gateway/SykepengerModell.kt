package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway

import java.time.LocalDate

data class GjenståendeSykedager(
    val grunnlag: GjenståendeSykedagerGrunnlag,
    val gjenståendeDager: Int? = null,
    val vurdertTilOgMed: LocalDate? = null,
)

enum class GjenståendeSykedagerGrunnlag {
    VEDTAK_I_NY_LØSNING,
    SISTE_UTBETALING_I_INFOTRYGD,
    MAKSDATO_I_INFOTRYGD,
    MANGLER_MAKSDATO_I_INFOTRYGD,
    INGEN_SYKEPENGER_SISTE_26_UKER
}

data class SykepengerResponse(
    val utbetaltePerioder: List<UtbetaltePerioder>
)

data class UtbetaltePerioder(
    val fom: LocalDate,
    val tom: LocalDate,
    val grad: Number,
    val organisasjonsnummer: String?
)