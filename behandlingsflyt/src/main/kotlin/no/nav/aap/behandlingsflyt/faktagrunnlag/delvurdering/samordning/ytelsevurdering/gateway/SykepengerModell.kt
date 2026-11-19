package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Hverdager
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Hverdager.Companion.plusHverdager
import java.time.LocalDate

data class GjenståendeSykedager(
    val grunnlag: GjenståendeSykedagerGrunnlag,
    val gjenståendeDager: Int? = null,
    val vurdertTilOgMed: LocalDate? = null,
) {
    fun utledMaksdato(): LocalDate? {
        if (grunnlag != GjenståendeSykedagerGrunnlag.VEDTAK_I_NY_LØSNING) return null
        if (gjenståendeDager == null || vurdertTilOgMed == null) return null
        return vurdertTilOgMed.plusHverdager(Hverdager(gjenståendeDager))
    }
}

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