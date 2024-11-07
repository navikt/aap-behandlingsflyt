package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.år

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektPerÅr
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.Prosent
import java.time.LocalDate
import java.time.Year
import java.util.SortedSet

class Inntektsbehov(private val input: Input) {

    fun utledAlleRelevanteÅr(): Set<Year> {
        return input.datoerForInnhenting()
            .flatMap(::treÅrForutFor)
            .toSortedSet()
    }

    fun hentYtterligereNedsattArbeidsevneDato(): LocalDate? {
        return input.beregningVurdering?.ytterligereNedsattArbeidsevneDato
    }

    private fun treÅrForutFor(nedsettelsesdato: LocalDate): SortedSet<Year> {
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

    /**
     * Skal beregne med uføre om det finnes data på uføregrad.
     */
    fun finnesUføreData(): Boolean {
        return input.beregningVurdering?.ytterligereNedsattArbeidsevneDato != null && input.uføregrad != null
    }

    /**
     * Om det eksisterer informasjon om yrkesskade (tidspunkt og andel nedsettelse) og det finnes en antatt årlig
     * inntekt, så skal beregningen skje med yrkesskadefordel (§11-22)
     */
    fun yrkesskadeVurderingEksisterer(): Boolean {
        return input.yrkesskadevurdering?.relevanteSaker?.isNotEmpty() == true && input.beregningVurdering?.antattÅrligInntekt != null && input.yrkesskadevurdering.andelAvNedsettelse != null
    }

    /**
     * Gitt en mengde med inntekter [inntekter] og en [nedsettelsesdato], returner en mengde med
     * relevante inntekter (3 år før nedsettelsesdato). Om inntekten ikke finnes, antas den å være
     * lik 0.
     */
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
        val relevanteSaker = input.yrkesskadevurdering?.relevanteSaker ?: emptyList()

        return requireNotNull(relevanteSaker.map { sak -> input.registrerteYrkesskader?.yrkesskader?.singleOrNull { it.ref == sak } }
            .firstOrNull()?.skadedato)
    }

    fun antattÅrligInntekt(): Beløp {
        return requireNotNull(input.beregningVurdering?.antattÅrligInntekt)
    }

    fun andelYrkesskade(): Prosent {
        return requireNotNull(input.yrkesskadevurdering?.andelAvNedsettelse)
    }
}
