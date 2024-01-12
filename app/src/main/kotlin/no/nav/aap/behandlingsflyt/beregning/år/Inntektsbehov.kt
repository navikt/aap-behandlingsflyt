package no.nav.aap.behandlingsflyt.beregning.år

import no.nav.aap.behandlingsflyt.faktagrunnlag.inntekt.InntektPerÅr
import java.time.LocalDate
import java.time.Year

class Inntektsbehov(private val input: Input) {

    fun utledAlleRelevanteÅr(): Set<Year> {
        return input.datoerForInnhenting()
            .flatMap(::treÅrForutFor)
            .toSortedSet()
    }

    private fun treÅrForutFor(nedsettelsesdato: LocalDate): Set<Year> {
        val nedsettelsesår = Year.from(nedsettelsesdato)
        return 3.downTo(1L).map(nedsettelsesår::minusYears).toSortedSet()
    }

    fun utledForOrdinær(inntekter: Set<InntektPerÅr>): Set<InntektPerÅr> {
        return filtrerInntekter(input.nedsettelsesDato, inntekter)
    }

    fun utledForYtterligereNedsatt(inntekter: Set<InntektPerÅr>): Set<InntektPerÅr>? {
        val ytterligereNedsettelsesDato = input.ytterligereNedsettelsesDato
        if (ytterligereNedsettelsesDato == null) {
            return null
        }
        return filtrerInntekter(ytterligereNedsettelsesDato, inntekter)
    }

    private fun filtrerInntekter(
        nedsettelsesdato: LocalDate,
        inntekter: Set<InntektPerÅr>
    ): Set<InntektPerÅr> {
        val relevanteÅr = treÅrForutFor(nedsettelsesdato)
        return inntekter.filter { inntektPerÅr -> inntektPerÅr.år in relevanteÅr }.toSortedSet()
    }
}
