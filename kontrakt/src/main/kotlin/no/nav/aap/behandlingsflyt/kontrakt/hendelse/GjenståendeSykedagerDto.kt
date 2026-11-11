package no.nav.aap.behandlingsflyt.kontrakt.hendelse

import java.time.LocalDate

public data class GjenståendeSykedagerDto(
    val grunnlag: GjenståendeSykedagerGrunnlagDto,
    val gjenståendeDager: Int?,
    val vurdertTilOgMed: LocalDate?,
)

public enum class GjenståendeSykedagerGrunnlagDto {
    VEDTAK_I_NY_LØSNING,
    SISTE_UTBETALING_I_INFOTRYGD,
    MAKSDATO_I_INFOTRYGD,
    MANGLER_MAKSDATO_I_INFOTRYGD,
    INGEN_SYKEPENGER_SISTE_26_UKER
}
