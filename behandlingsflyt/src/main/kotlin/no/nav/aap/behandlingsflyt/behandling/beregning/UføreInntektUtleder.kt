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

/**
 * Delperiode innen ett år der uføregraden er konstant (ett segment av uføre-tidslinjen klippet
 * til året). Brukes til å la saksbehandler legge inn beregnet PGI per delperiode, og til å vise
 * uføregrad-segmentene i kortet.
 */
data class UføregradDelperiode(
    val periode: Periode,
    val uføregrad: Prosent,
)

/**
 * Avgjør når saksbehandler må legge inn manuell periodeinntekt fordi uføregraden endrer seg midt
 * i et beregningsår og A-inntekt (månedsinntekt) og POPP (årsinntekt) er uenige.
 */
object UføreInntektUtleder {

    // Samme avviksgrense som [InntektValidering]: avvik >= 100 kr betyr at vi ikke stoler på
    // automatisk sammenslåing av A-inntekt og årsinntekt.
    private val AVVIKSGRENSE = BigDecimal(100)

    /**
     * Returnerer årene som krever manuell periodeinntekt: år med variabel uføregrad (endring midt
     * i året — ikke konstant hele året) OG der summen av månedsinntekt (A-inntekt) avviker fra
     * årsinntekt (POPP). Vurderer kun de tre årene forut for [ytterligereNedsattDato].
     */
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

    /**
     * Deler [år] opp i delperioder etter uføregrad-segmentene (f.eks. «jan–feb» @ 0 % og
     * «mar–des» @ 50 %). Perioder uten registrert uføregrad fylles med 0 % slik at hele året dekkes
     * — samme antagelse som månedsberegningen i [UføreBeregning] (måned uten segment = 0 %).
     */
    fun utledDelperioder(uføregrader: Set<Uføre>, år: Year): List<UføregradDelperiode> {
        if (uføregrader.isEmpty()) return emptyList()
        val detteÅret = årsperiode(år)
        val uføreSegmenter = uføregrader.tilTidslinje()
            .begrensetTil(detteÅret)
            .komprimer()
            .segmenter()
            .map { UføregradDelperiode(it.periode, it.verdi) }
            .sortedBy { it.periode.fom }

        if (uføreSegmenter.isEmpty()) return emptyList()

        val medHull = mutableListOf<UføregradDelperiode>()
        var cursor = detteÅret.fom
        for (segment in uføreSegmenter) {
            if (segment.periode.fom > cursor) {
                medHull.add(UføregradDelperiode(Periode(cursor, segment.periode.fom.minusDays(1)), Prosent.`0_PROSENT`))
            }
            medHull.add(segment)
            cursor = segment.periode.tom.plusDays(1)
        }
        if (cursor <= detteÅret.tom) {
            medHull.add(UføregradDelperiode(Periode(cursor, detteÅret.tom), Prosent.`0_PROSENT`))
        }
        return medHull
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
