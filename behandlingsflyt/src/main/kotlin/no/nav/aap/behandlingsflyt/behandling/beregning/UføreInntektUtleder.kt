package no.nav.aap.behandlingsflyt.behandling.beregning

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektPerÅr
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.Uføre
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.tilTidslinje
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Prosent
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Year

data class UføregradDelperiode(
    val periode: Periode,
    val uføregrad: Prosent,
)

/**
 * Avgjør når saksbehandler må legge inn manuell periodeinntekt fordi uføregraden endrer seg midt
 * i et beregningsår og A-inntekt (månedsinntekt) og POPP (årsinntekt) er forskjellig.
 */
object UføreInntektUtleder {
    private val AVVIKSGRENSE = BigDecimal(100)

    fun finnÅrSomKreverManuellPeriodeinntekt(
        uføregrader: Set<Uføre>,
        inntektPerMåned: Set<Månedsinntekt>,
        årsInntekter: Set<InntektPerÅr>,
        ytterligereNedsattDato: LocalDate,
    ): Set<Year> {
        if (uføregrader.isEmpty()) return emptySet()
        val uføreTidslinje = uføregrader.tilTidslinje()

        return Beregning.treÅrForutFor(ytterligereNedsattDato)
            .filter { år ->
                harVariabelUføregrad(uføreTidslinje, år) && harInntektsavvik(år, inntektPerMåned, årsInntekter)
            }
            .toSet()
    }

    fun utledDelperioder(uføregrader: Set<Uføre>, år: Year): List<UføregradDelperiode> {
        if (uføregrader.isEmpty()) return emptyList()

        return uføregrader.tilTidslinje()
            .begrensetTil(årsperiode(år))
            .komprimer()
            .segmenter()
            .map { UføregradDelperiode(it.periode, it.verdi) }
            .sortedBy { it.periode.fom }
    }

    private fun harVariabelUføregrad(uføreTidslinje: Tidslinje<Prosent>, år: Year): Boolean {
        val detteÅret = årsperiode(år)
        val begrenset = uføreTidslinje.begrensetTil(detteÅret)
        if (begrenset.isEmpty()) return false
        val konstantHeleÅret = begrenset.helePerioden() == detteÅret && begrenset.verdier().toSet().size == 1
        return !konstantHeleÅret
    }

    private fun harInntektsavvik(
        år: Year,
        inntektPerMåned: Set<Månedsinntekt>,
        årsInntekter: Set<InntektPerÅr>,
    ): Boolean {
        val årsInntekt = årsInntekter.firstOrNull { it.år == år }?.beløp ?: return false
        val summertMåned = inntektPerMåned
            .filter { Year.of(it.årMåned.year) == år }
            .sumOf { it.beløp.verdi }
        val differanse = (årsInntekt.verdi.stripTrailingZeros() - summertMåned.stripTrailingZeros()).abs()
        return differanse >= AVVIKSGRENSE
    }

    private fun årsperiode(år: Year): Periode = Periode(år.atDay(1), år.atMonth(12).atEndOfMonth())
}
