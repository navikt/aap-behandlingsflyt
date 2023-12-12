package no.nav.aap.behandlingsflyt.underveis.regler

import no.nav.aap.behandlingsflyt.beregning.Prosent
import no.nav.aap.behandlingsflyt.underveis.tidslinje.Segment
import no.nav.aap.behandlingsflyt.underveis.tidslinje.StandardSammenslåere
import no.nav.aap.behandlingsflyt.underveis.tidslinje.Tidslinje
import java.math.BigDecimal
import java.time.Period
import java.util.*

private const val ANTALL_DAGER_I_MELDEPERIODE = 14
private const val ANTALL_TIMER_I_ARBEIDSUKE = 37.5
private const val HØYESTE_GRADERING_NORMAL = 60
private const val HØYESTE_GRADERING_OPPTRAPPING = 80

/**
 * Graderer arbeid der hvor det ikke er avslått pga en regel tidliger i løpet
 *
 * - Arbeid fra meldeplikt
 */
class GraderingArbeidRegel : UnderveisRegel {
    override fun vurder(input: UnderveisInput, resultat: Tidslinje<Vurdering>): Tidslinje<Vurdering> {
        // TODO: Legge inn noe reelt
        val arbeidsTidslinje =
            Tidslinje(resultat.splittOppEtter(Period.ofDays(1))
                .segmenter()
                .map { it.periode }
                .map { Segment(it, TimerArbeid(BigDecimal.ZERO)) })
                .splittOppOgMapOmEtter(Period.ofDays(ANTALL_DAGER_I_MELDEPERIODE)) { arbeidsSegmenter ->
                    regnUtGradering(arbeidsSegmenter)
                }.komprimer()

        val opptrappingTidslinje =
            Tidslinje(input.opptrappingPerioder.map { Segment(it, Prosent(HØYESTE_GRADERING_OPPTRAPPING)) })

        val grenseverdiGradering = resultat.mapValue { Prosent(HØYESTE_GRADERING_NORMAL) }
            .kombiner(opptrappingTidslinje, StandardSammenslåere.prioriterHøyreSide())

        return resultat.kombiner(grenseverdiGradering, { periode, venstreSegment, høyreSegment ->
            var vurdering: Vurdering? = venstreSegment?.verdi
            if (høyreSegment?.verdi != null) {
                vurdering = vurdering?.leggTilGrenseverdi(høyreSegment.verdi)
            }
            Segment(periode, vurdering)
        }).kombiner(arbeidsTidslinje, { periode, venstreSegment, høyreSegment ->
            var vurdering: Vurdering? = venstreSegment?.verdi
            if (høyreSegment?.verdi != null) {
                vurdering = vurdering?.leggTilGradering(høyreSegment.verdi)
            }
            Segment(periode, vurdering)
        })
    }

    private fun regnUtGradering(arbeidsSegmenter: NavigableSet<Segment<TimerArbeid>>): NavigableSet<Segment<Gradering>> {
        val antallTimerArbeid = arbeidsSegmenter.sumOf { it.verdi?.antallTimer ?: BigDecimal.ZERO }
        val gradering =
            Prosent(antallTimerArbeid.divide(BigDecimal(ANTALL_TIMER_I_ARBEIDSUKE).multiply(BigDecimal.TWO)).toInt())
        return TreeSet(arbeidsSegmenter.map { segment ->
            Segment(
                segment.periode, Gradering(
                    segment?.verdi ?: TimerArbeid(
                        BigDecimal.ZERO
                    ), gradering
                )
            )
        })
    }
}