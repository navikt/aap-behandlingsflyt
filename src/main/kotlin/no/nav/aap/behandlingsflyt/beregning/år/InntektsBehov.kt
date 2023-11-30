package no.nav.aap.behandlingsflyt.beregning.år

import no.nav.aap.behandlingsflyt.faktagrunnlag.inntekt.adapter.InntektPerÅr
import java.time.LocalDate
import java.time.Year

class InntektsBehov(private val input: Input) {

    fun utledAlleRelevanteÅr(): Set<Year> {
        return input.datoerForInnhenting()
            .flatMap { treÅrForutFor(it) }
            .toSortedSet()
    }

    private fun treÅrForutFor(nedsettelsesDato: LocalDate): Set<Year> {
        val nedsettelsesÅr = Year.of(nedsettelsesDato.year)

        return setOf(nedsettelsesÅr.minusYears(3), nedsettelsesÅr.minusYears(2), nedsettelsesÅr.minusYears(1))
    }

    fun utledForOrdinær(inntekter: Set<InntektPerÅr>): List<InntektPerÅr> {
        val relevanteÅr = treÅrForutFor(input.nedsettelsesDato)
        return inntekter.filter { relevanteÅr.contains(it.år) }
    }
}