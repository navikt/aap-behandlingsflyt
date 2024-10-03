package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Gradering
import no.nav.aap.tidslinje.JoinStyle
import no.nav.aap.tidslinje.Segment
import no.nav.aap.tidslinje.StandardSammenslåere
import no.nav.aap.tidslinje.Tidslinje
import no.nav.aap.verdityper.Prosent
import no.nav.aap.verdityper.TimerArbeid
import java.math.BigDecimal
import java.math.RoundingMode
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
        val pliktkortTidslinje = konstruerTidslinje(input)

        // Regner kun ut gradering for perioden det er sendt noe inn for
        val arbeidsTidslinje =
            pliktkortTidslinje.splittOppOgMapOmEtter(Period.ofDays(ANTALL_DAGER_I_MELDEPERIODE))
            { arbeidsSegmenter ->
                regnUtGradering(arbeidsSegmenter)
            }.komprimer()

        val opptrappingTidslinje =
            Tidslinje(input.opptrappingPerioder.map { Segment(it, Prosent(HØYESTE_GRADERING_OPPTRAPPING)) })

        val grenseverdiGradering = resultat.mapValue { Prosent(HØYESTE_GRADERING_NORMAL) }
            .kombiner(opptrappingTidslinje, StandardSammenslåere.prioriterHøyreSideCrossJoin())

        return resultat.kombiner(grenseverdiGradering, JoinStyle.OUTER_JOIN{ periode, venstreSegment, høyreSegment ->
            var vurdering = venstreSegment?.verdi
            if (høyreSegment != null) {
                vurdering = vurdering?.leggTilGrenseverdi(høyreSegment.verdi)
            }
            Segment(periode, vurdering)
        }).kombiner(arbeidsTidslinje,JoinStyle.OUTER_JOIN { periode, venstreSegment, høyreSegment ->
            var vurdering: Vurdering = venstreSegment?.verdi ?: Vurdering()
            if (høyreSegment != null) {
                vurdering = vurdering.leggTilGradering(høyreSegment.verdi)
            }
            Segment(periode, vurdering)
        })
    }

    private fun konstruerTidslinje(input: UnderveisInput): Tidslinje<TimerArbeid> {
        var tidslinje = Tidslinje(listOf(Segment(input.rettighetsperiode, TimerArbeid(BigDecimal.ZERO))))
        for (pliktkort in input.pliktkort) {
            tidslinje = tidslinje.kombiner(Tidslinje(pliktkort.timerArbeidPerPeriode.map {
                Segment(
                    it.periode,
                    it.arbeidPerDag() // Smører timene meldt over alle dagene de er meldt for
                )
            }), StandardSammenslåere.prioriterHøyreSide())
        }
        return tidslinje.splittOppEtter(Period.ofDays(1))
    }

    private fun regnUtGradering(arbeidsSegmenter: NavigableSet<Segment<TimerArbeid>>): NavigableSet<Segment<Gradering>> {
        val antallTimerArbeid = arbeidsSegmenter.sumOf { it.verdi.antallTimer }
        val ethundre = BigDecimal.valueOf(100)
        val andelArbeid =
            Prosent(
                antallTimerArbeid.divide(
                    BigDecimal(ANTALL_TIMER_I_ARBEIDSUKE).multiply(BigDecimal.TWO),
                    3,
                    RoundingMode.HALF_UP
                ).multiply(ethundre).toInt()
            )
        return TreeSet(arbeidsSegmenter.map { segment ->
            Segment(
                segment.periode, Gradering(
                    segment?.verdi ?: TimerArbeid(
                        BigDecimal.ZERO
                    ), andelArbeid, Prosent.`100_PROSENT`.minus(andelArbeid)
                )
            )
        })
    }
}