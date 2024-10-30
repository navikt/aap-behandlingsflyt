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

// § 11-23 tredje ledd
private const val ANTALL_TIMER_I_ARBEIDSUKE = 37.5

private val ANTALL_TIMER_I_MELDEPERIODE =
    BigDecimal(ANTALL_TIMER_I_ARBEIDSUKE).multiply(BigDecimal.TWO)

// § 11-23 fjerde ledd
private const val HØYESTE_GRADERING_NORMAL = 60

// § 11-23 sjette ledd
private const val HØYESTE_GRADERING_OPPTRAPPING = 80

/** § 11-23. Reduksjon ved delvis nedsatt arbeidsevne
 *
 * Graderer arbeid der hvor det ikke er avslått pga en regel tidliger i løpet
 *
 * - Arbeid fra meldeplikt
 */
class GraderingArbeidRegel : UnderveisRegel {
    override fun vurder(input: UnderveisInput, resultat: Tidslinje<Vurdering>): Tidslinje<Vurdering> {
        val pliktkortTidslinje = konstruerTidslinje(input)

        // Regner kun ut gradering for perioden det er sendt noe inn for
        /* TODO: gjenbruk meldeperiode som er regnet ut i [MeldepliktRegel]. */
        val arbeidsTidslinje =
            pliktkortTidslinje.splittOppOgMapOmEtter(Period.ofDays(ANTALL_DAGER_I_MELDEPERIODE))
            { arbeidsSegmenter ->
                regnUtGradering(arbeidsSegmenter)
            }.komprimer()

        // § 11-23 sjette ledd
        val opptrappingTidslinje =
            Tidslinje(input.opptrappingPerioder.map { Segment(it, Prosent(HØYESTE_GRADERING_OPPTRAPPING)) })

        val grenseverdiGraderinger = resultat.mapValue { Prosent(HØYESTE_GRADERING_NORMAL) }
            .kombiner(opptrappingTidslinje, StandardSammenslåere.prioriterHøyreSideCrossJoin())

        return resultat.kombiner(grenseverdiGraderinger, JoinStyle.OUTER_JOIN { periode, foreløpigVurdering, grenseverdiGradering ->
            var vurdering = foreløpigVurdering?.verdi
            if (grenseverdiGradering != null) {
                vurdering = vurdering?.leggTilGrenseverdi(grenseverdiGradering.verdi)
            }
            Segment(periode, vurdering)
        }).kombiner(arbeidsTidslinje, JoinStyle.OUTER_JOIN { periode, foreløpigVurdering, gradering ->
            var vurdering: Vurdering = foreløpigVurdering?.verdi ?: Vurdering()
            if (gradering != null) {
                vurdering = vurdering.leggTilGradering(gradering.verdi)
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

    /** § 11-23 tredje ledd */
    private fun regnUtGradering(arbeidsSegmenter: NavigableSet<Segment<TimerArbeid>>): NavigableSet<Segment<Gradering>> {
        val arbeidetTidIMeldeperioden = arbeidsSegmenter.sumOf { it.verdi.antallTimer }

        val andelArbeid = Prosent.fraDesimal(
            arbeidetTidIMeldeperioden.divide(ANTALL_TIMER_I_MELDEPERIODE, 3, RoundingMode.HALF_UP)
        )
        return TreeSet(arbeidsSegmenter.map { segment ->
            Segment(
                segment.periode,
                Gradering(
                    totaltAntallTimer = segment?.verdi ?: TimerArbeid(BigDecimal.ZERO),
                    andelArbeid = andelArbeid,
                    gradering = Prosent.`100_PROSENT`.minus(andelArbeid)
                )
            )
        })
    }
}