package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Hverdager
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Hverdager.Companion.plusHverdager
import java.time.DayOfWeek
import java.time.LocalDate

data class GjenståendeSykedager(
    val grunnlag: GjenståendeSykedagerGrunnlag,
    val gjenståendeDager: Int? = null,
    val vurdertTilOgMed: LocalDate? = null,
) {
    fun utledMaksdato(): LocalDate? {
        if (gjenståendeDager == null || vurdertTilOgMed == null) return null
        return when (grunnlag) {
            GjenståendeSykedagerGrunnlag.VEDTAK_I_NY_LØSNING,
            GjenståendeSykedagerGrunnlag.SISTE_UTBETALING_I_INFOTRYGD,
            GjenståendeSykedagerGrunnlag.MAKSDATO_I_INFOTRYGD -> {
                if (gjenståendeDager == 0) {
                    vurdertTilOgMed
                } else {
                    // plusHverdager teller startdatoen som element 0, så helgedatoer må flyttes til fredag
                    val startdag = when (vurdertTilOgMed.dayOfWeek) {
                        DayOfWeek.SATURDAY -> vurdertTilOgMed.minusDays(1)
                        DayOfWeek.SUNDAY -> vurdertTilOgMed.minusDays(2)
                        else -> vurdertTilOgMed
                    }
                    startdag.plusHverdager(Hverdager(gjenståendeDager))
                }
            }

            GjenståendeSykedagerGrunnlag.MANGLER_MAKSDATO_I_INFOTRYGD,
            GjenståendeSykedagerGrunnlag.INGEN_SYKEPENGER_SISTE_26_UKER -> null
        }
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