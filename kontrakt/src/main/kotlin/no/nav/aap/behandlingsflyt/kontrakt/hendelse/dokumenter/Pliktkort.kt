package no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDate
import java.time.temporal.ChronoUnit

public sealed interface Pliktkort : Melding {
    public fun fom(): LocalDate?
    public fun tom(): LocalDate?
}

@JsonIgnoreProperties(ignoreUnknown = true)
public data class PliktkortV0(
    public val harDuArbeidet: Boolean,
    public val timerArbeidPerPeriode: List<ArbeidIPeriode>,
) : Pliktkort {

    init {
        require(!overlappendePerioder(timerArbeidPerPeriode)) {
            "kan ikke gi overlappende opplysninger i ett meldekort"
        }

        require(timerArbeidetSamsvarerMedArbeidetSvar(harDuArbeidet, timerArbeidPerPeriode)) {
            "oppgitte timer arbeidet samsvarer ikke til svar på `harDuArbeidet`"
        }
    }

    override fun fom(): LocalDate? {
        return timerArbeidPerPeriode.minOfOrNull { it.fraOgMedDato }
    }

    override fun tom(): LocalDate? {
        return timerArbeidPerPeriode.maxOfOrNull { it.fraOgMedDato }
    }

    public companion object {
        public fun overlappendePerioder(timerArbeidPerPeriode: List<ArbeidIPeriode>): Boolean {
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
            timerArbeidPerPeriode: List<ArbeidIPeriode>
        ): Boolean {
            return if (harDuArbeidet) {
                timerArbeidPerPeriode.sumOf { it.timerArbeid } > 0.0
            } else {
                timerArbeidPerPeriode.all { it.timerArbeid == 0.0 }
            }
        }
    }
}

public data class ArbeidIPeriode(
    val fraOgMedDato: LocalDate,
    val tilOgMedDato: LocalDate,
    val timerArbeid: Double,
) {
    init {
        require(gyldigTimerArbeidet(timerArbeid, fraOgMedDato, tilOgMedDato)) {
            "må ha arbeidet mellom 0 og 24 * antallDager timer"
        }
    }

    public fun overlapper(other: ArbeidIPeriode): Boolean {
        return this.fraOgMedDato <= other.tilOgMedDato && other.fraOgMedDato <= this.tilOgMedDato
    }

    public companion object {
        public fun gyldigTimerArbeidet(timer: Double, fom: LocalDate, tom: LocalDate): Boolean  {
            return timer >= 0 && timer <= 24.0 * antallDager(fom, tom)
        }
    }
}

private fun antallDager(fom: LocalDate, tom: LocalDate): Int {
    return fom.until(tom.plusDays(1), ChronoUnit.DAYS).toInt()
}
