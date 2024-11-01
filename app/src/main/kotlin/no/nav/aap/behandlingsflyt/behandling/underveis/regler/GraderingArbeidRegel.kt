package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Gradering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.ArbeidsevneVurdering.Companion.tidslinje
import no.nav.aap.tidslinje.JoinStyle
import no.nav.aap.tidslinje.Segment
import no.nav.aap.tidslinje.StandardSammenslåere
import no.nav.aap.tidslinje.Tidslinje
import no.nav.aap.verdityper.Prosent
import no.nav.aap.verdityper.Prosent.Companion.`0_PROSENT`
import no.nav.aap.verdityper.TimerArbeid
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Period
import java.util.*

/*

- Skal vi legge vurderingen av andre ledd inn i Vurdering, og så sammenstille senere?
- Hvordan sammenstille.


arbeidsevne: 30%
faktisk arbeid: 40%
utbetaling %: 60%

arbeidsevne: 30%
faktisk arbeid: 20%
utbetaling %: 70%

gradering = max(arbeidsevne, faktisk arbeid)
utbetaling = 100 % - max(arbeidsevne, faktisk arbeid)
 */


/*
 TODO: gjenbruk meldeperiode som er regnet ut i [MeldepliktRegel].
 TODO: flytt gradering til metode basert på data.
 TODO: skal arbeidsevne cappes av grenseverdier  e.l.?
 TODO: Flytte repository til primary constructor i Løsere
 TODO: skal reduksjon grunnet instutisjonsvurdering kunne føre til negativ gradering?
*/


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
        val arbeidsTidslinje = graderingerTidslinje(input)
        val graderingsgrenseverdier = graderingsgrenseverdier(input, resultat)

        return resultat
            .leggTilVurderinger(arbeidsTidslinje, Vurdering::leggTilGradering)
            .leggTilVurderinger(graderingsgrenseverdier, Vurdering::leggTilGrenseverdi)
    }

    private fun graderingerTidslinje(input: UnderveisInput): Tidslinje<Gradering> {
        val timerArbeidetTidslinje = timerArbeidetTidslinje(input)
        val arbeidsevneVurdering = input.arbeidsevneGrunnlag.vurderinger.tidslinje()
            .kryss(input.rettighetsperiode)

        // Regner kun ut gradering for perioden det er sendt noe inn for
        val arbeidsTidslinje = timerArbeidetTidslinje.kombiner(
            arbeidsevneVurdering, JoinStyle.OUTER_JOIN { periode, pliktkort, arbeidsevne ->
                Segment(
                    periode,
                    Arbeid(
                        timerArbeid = pliktkort?.verdi,
                        arbeidsevne = arbeidsevne?.verdi?.arbeidsevne
                    )
                )
            })
            .splittOppOgMapOmEtter(Period.ofDays(ANTALL_DAGER_I_MELDEPERIODE)) { meldeperiode ->
                regnUtGradering(meldeperiode)
            }.komprimer()
        return arbeidsTidslinje
    }

    private fun graderingsgrenseverdier(
        input: UnderveisInput,
        resultat: Tidslinje<Vurdering>
    ): Tidslinje<Prosent> {
        val opptrappingTidslinje = Tidslinje(input.opptrappingPerioder.map { Segment(it, Prosent(HØYESTE_GRADERING_OPPTRAPPING)) })

        return resultat.mapValue { Prosent(HØYESTE_GRADERING_NORMAL) }
            .kombiner(opptrappingTidslinje, StandardSammenslåere.prioriterHøyreSideCrossJoin())
    }

    private fun timerArbeidetTidslinje(input: UnderveisInput): Tidslinje<TimerArbeid> {
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
    private class Arbeid(
        val timerArbeid: TimerArbeid?,
        val arbeidsevne: Prosent?,
    )

    private fun regnUtGradering(arbeidsSegmenter: NavigableSet<Segment<Arbeid>>): NavigableSet<Segment<Gradering>> {
        val arbeidetTidIMeldeperioden = arbeidsSegmenter.sumOf { it.verdi.timerArbeid?.antallTimer ?: BigDecimal.ZERO }

        val andelArbeid = Prosent.fraDesimal(
            arbeidetTidIMeldeperioden.divide(ANTALL_TIMER_I_MELDEPERIODE, 3, RoundingMode.HALF_UP)
        )
        return TreeSet(arbeidsSegmenter.map { segment ->
            val fastsattArbeidsevne = segment?.verdi?.arbeidsevne ?: `0_PROSENT`
            Segment(
                segment.periode,
                Gradering(
                    totaltAntallTimer = segment?.verdi?.timerArbeid ?: TimerArbeid(BigDecimal.ZERO),
                    andelArbeid = andelArbeid,
                    fastsattArbeidsevne = fastsattArbeidsevne,
                    gradering = Prosent.`100_PROSENT`.minus(
                        Prosent.max(andelArbeid, fastsattArbeidsevne)
                    ),
                )
            )
        })
    }
}