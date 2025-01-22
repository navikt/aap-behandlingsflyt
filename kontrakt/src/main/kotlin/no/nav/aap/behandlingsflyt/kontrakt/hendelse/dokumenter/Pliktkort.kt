package no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDate

public sealed interface Pliktkort : Melding

@JsonIgnoreProperties(ignoreUnknown = true)
public data class PliktkortV0(public val timerArbeidPerPeriode: List<ArbeidIPeriode>) : Pliktkort

public data class ArbeidIPeriode(val fraOgMedDato: LocalDate, val tilOgMedDato: LocalDate, val timerArbeid: Double)
