package no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDate

public sealed interface Pliktkort : Melding {
    public fun fom(): LocalDate?
    public fun tom(): LocalDate?
}

@JsonIgnoreProperties(ignoreUnknown = true)
public data class PliktkortV0(public val timerArbeidPerPeriode: List<ArbeidIPeriode>) : Pliktkort {
    override fun fom(): LocalDate? {
        return timerArbeidPerPeriode.minOfOrNull { it.fraOgMedDato }
    }

    override fun tom(): LocalDate? {
        return timerArbeidPerPeriode.maxOfOrNull { it.fraOgMedDato }
    }
}

public data class ArbeidIPeriode(val fraOgMedDato: LocalDate, val tilOgMedDato: LocalDate, val timerArbeid: Double)
