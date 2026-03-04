package no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDate
import java.time.temporal.ChronoUnit

public sealed interface Meldekort : Melding {
    public fun fom(): LocalDate?
    public fun tom(): LocalDate?
}

@JsonIgnoreProperties(ignoreUnknown = true)
public data class MeldekortV0(
    public val harDuArbeidet: Boolean,
    public val timerArbeidPerPeriode: List<ArbeidIPeriodeV0>,
    public val fravær: List<FraværIPeriodeV0>?,
) : Meldekort {

    init {
        require(!overlappendePerioder(timerArbeidPerPeriode)) {
            "kan ikke gi overlappende opplysninger i et meldekort"
        }

        require(timerArbeidetSamsvarerMedArbeidetSvar(harDuArbeidet, timerArbeidPerPeriode)) {
            "oppgitte timer arbeidet samsvarer ikke til svar på `harDuArbeidet`"
        }
    }

    override fun fom(): LocalDate? {
        return listOfNotNull(
            timerArbeidPerPeriode.minOfOrNull { it.fraOgMedDato },
            fravær?.minOfOrNull { it.dato }
        ).minOrNull()
    }

    override fun tom(): LocalDate? {
        return listOfNotNull(
            timerArbeidPerPeriode.maxOfOrNull { it.tilOgMedDato },
            fravær?.maxOfOrNull { it.dato }
        ).maxOrNull()
    }

    public companion object {
        public fun overlappendePerioder(timerArbeidPerPeriode: List<ArbeidIPeriodeV0>): Boolean {
            for (i in timerArbeidPerPeriode.indices) {
                for (j in i + 1..<timerArbeidPerPeriode.size) {
                    if (timerArbeidPerPeriode[i].overlapper(timerArbeidPerPeriode[j])) {
                        return true
                    }
                }
            }
            return false
        }

        public fun timerArbeidetSamsvarerMedArbeidetSvar(
            harDuArbeidet: Boolean,
            timerArbeidPerPeriode: List<ArbeidIPeriodeV0>
        ): Boolean {
            return if (harDuArbeidet) {
                timerArbeidPerPeriode.sumOf { it.timerArbeid } > 0.0
            } else {
                timerArbeidPerPeriode.all { it.timerArbeid == 0.0 }
            }
        }
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
public data class ArbeidIPeriodeV0(
    val fraOgMedDato: LocalDate,
    val tilOgMedDato: LocalDate,
    val timerArbeid: Double,
) {
    init {
        require(gyldigTimerArbeidet(timerArbeid, fraOgMedDato, tilOgMedDato)) {
            "må ha arbeidet mellom 0 og 24 * antallDager timer"
        }
    }

    public fun overlapper(other: ArbeidIPeriodeV0): Boolean {
        return this.fraOgMedDato <= other.tilOgMedDato && other.fraOgMedDato <= this.tilOgMedDato
    }

    public companion object {
        public fun gyldigTimerArbeidet(timer: Double, fom: LocalDate, tom: LocalDate): Boolean  {
            return timer >= 0 && timer <= 24.0 * antallDager(fom, tom)
        }
    }
}

public data class FraværIPeriodeV0(
    val dato: LocalDate,
    val fraværÅrsak: FraværÅrsakV0,
)

enum class FraværÅrsakV0 {
    SYKDOM_ELLER_SKADE,
    OMSORG_FØRSTE_SKOLEDAG_TILVENNING_ELLER_ANNEN_OPPFØLGING_BARN,
    OMSORG_PLEIE_I_HJEMMET_AV_NÆR_PÅRØRENDE,
    OMSORG_DØDSFALL_I_FAMILIE_ELLER_VENNEKRETS,
    OMSORG_MEDDOMMER_ELLER_ANDRE_OFFENTLIGE_PLIKTER,
    OMSORG_ANNEN_STERK_GRUNN,
    ANNET
    ;
}

private fun antallDager(fom: LocalDate, tom: LocalDate): Int {
    return fom.until(tom.plusDays(1), ChronoUnit.DAYS).toInt()
}
