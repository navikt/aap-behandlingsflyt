package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.år

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektPerÅr
import no.nav.aap.verdityper.Beløp
import no.nav.aap.verdityper.Prosent
import java.time.LocalDate
import java.time.Year

class Inntektsbehov(private val input: Input) {

    fun utledAlleRelevanteÅr(): Set<Year> {
        return input.datoerForInnhenting()
            .flatMap(::treÅrForutFor)
            .toSortedSet()
    }

    fun hentYtterligereNedsattArbeidsevneDato(): LocalDate? {
        return input.beregningVurdering?.ytterligereNedsattArbeidsevneDato
    }

    private fun treÅrForutFor(nedsettelsesdato: LocalDate): Set<Year> {
        val nedsettelsesår = Year.from(nedsettelsesdato)
        return 3.downTo(1L).map(nedsettelsesår::minusYears).toSortedSet()
    }

    fun utledForOrdinær(): Set<InntektPerÅr> {
        return filtrerInntekter(input.nedsettelsesDato, input.inntekter)
    }

    fun utledForYtterligereNedsatt(): Set<InntektPerÅr> {
        val ytterligereNedsettelsesDato = input.beregningVurdering?.ytterligereNedsattArbeidsevneDato
        requireNotNull(ytterligereNedsettelsesDato)
        return filtrerInntekter(ytterligereNedsettelsesDato, input.inntekter)
    }

    fun skalBeregneMedUføre(): Boolean {
        return input.beregningVurdering?.ytterligereNedsattArbeidsevneDato != null && input.uføregrad != null
    }

    fun skalBeregneMedYrkesskadeFordel(): Boolean {
        return input.yrkesskadevurdering?.skadetidspunkt != null && input.beregningVurdering?.antattÅrligInntekt != null && input.yrkesskadevurdering.andelAvNedsettelse != null
    }

    private fun filtrerInntekter(
        nedsettelsesdato: LocalDate,
        inntekter: Set<InntektPerÅr>
    ): Set<InntektPerÅr> {
        val relevanteÅr = treÅrForutFor(nedsettelsesdato)
        return relevanteÅr.map { relevantÅr ->
            val år = inntekter.firstOrNull { entry -> entry.år == relevantÅr }
            if (år == null) {
                return@map InntektPerÅr(relevantÅr, Beløp(0))
            }
            return@map år
        }.toSet()
    }

    fun uføregrad(): Prosent {
        return requireNotNull(input.uføregrad)
    }

    fun skadetidspunkt(): LocalDate {
        return requireNotNull(input.yrkesskadevurdering?.skadetidspunkt)
    }

    fun antattÅrligInntekt(): Beløp {
        return requireNotNull(input.beregningVurdering?.antattÅrligInntekt)
    }

    fun andelYrkesskade(): Prosent {
        return requireNotNull(input.yrkesskadevurdering?.andelAvNedsettelse)
    }
}
