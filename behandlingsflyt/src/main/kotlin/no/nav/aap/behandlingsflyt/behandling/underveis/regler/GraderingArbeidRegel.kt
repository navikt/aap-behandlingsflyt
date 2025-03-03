package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.UtledMeldeperiodeRegel.Companion.groupByMeldeperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.ArbeidsGradering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.ArbeidsevneVurdering.Companion.tidslinje
import no.nav.aap.komponenter.tidslinje.JoinStyle
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.komponenter.verdityper.Prosent.Companion.`0_PROSENT`
import no.nav.aap.komponenter.verdityper.TimerArbeid
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Period

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
 TODO: Flytte repository til primary constructor i Løsere
 TODO: skal reduksjon grunnet instutisjonsvurdering kunne føre til negativ gradering?
*/


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
        require(input.rettighetsperiode.inneholder(resultat.helePerioden())) {
            "kan ikke vurdere utenfor rettighetsperioden fordi meldeperioden ikke er definert"
        }

        val arbeidsTidslinje = graderingerTidslinje(resultat, input)
        val graderingsgrenseverdier = graderingsgrenseverdier(input, resultat)

        return resultat
            .leggTilVurderinger(arbeidsTidslinje, Vurdering::leggTilGradering)
            .leggTilVurderinger(graderingsgrenseverdier, Vurdering::leggTilGrenseverdi)
    }

    private fun graderingerTidslinje(resultat: Tidslinje<Vurdering>, input: UnderveisInput): Tidslinje<ArbeidsGradering> {
        val timerArbeidetTidslinje = timerArbeidetTidslinje(input)
        val arbeidsevneVurdering = input.arbeidsevneGrunnlag.vurderinger.tidslinje()

        // Regner kun ut gradering for perioden det er sendt noe inn for
        val arbeidsTidslinje = timerArbeidetTidslinje.kombiner(
            arbeidsevneVurdering, JoinStyle.OUTER_JOIN { periode, meldekort, arbeidsevne ->
                Segment(
                    periode,
                    Arbeid(
                        timerArbeid = meldekort?.verdi,
                        arbeidsevne = arbeidsevne?.verdi?.arbeidsevne
                    )
                )
            })

        return groupByMeldeperiode(resultat, arbeidsTidslinje)
            .flatMap { meldeperiode ->
                regnUtGradering(meldeperiode.verdi)
            }
            .komprimer()
    }

    private fun graderingsgrenseverdier(
        input: UnderveisInput,
        resultat: Tidslinje<Vurdering>
    ): Tidslinje<Prosent> {
        val opptrappingTidslinje =
            Tidslinje(input.opptrappingPerioder.map { Segment(it, Prosent(HØYESTE_GRADERING_OPPTRAPPING)) })

        return resultat.mapValue { Prosent(HØYESTE_GRADERING_NORMAL) }
            .kombiner(opptrappingTidslinje, StandardSammenslåere.prioriterHøyreSideCrossJoin())
    }

    private fun timerArbeidetTidslinje(input: UnderveisInput): Tidslinje<TimerArbeid> {
        var tidslinje = Tidslinje(listOf(Segment(input.rettighetsperiode, TimerArbeid(BigDecimal.ZERO))))
        val innsendt = input.innsendingsTidspunkt.map { it.value to it.key }.toMap()
        for (meldekort in input.meldekort.sortedBy { innsendt[it.journalpostId] }) {
            tidslinje = tidslinje.kombiner(Tidslinje(meldekort.timerArbeidPerPeriode.map {
                Segment(
                    it.periode,
                    it.arbeidPerDag() // Smører timene meldt over alle dagene de er meldt for
                )
            }), StandardSammenslåere.prioriterHøyreSideCrossJoin())
        }
        return tidslinje.splittOppEtter(Period.ofDays(1))
    }

    /** § 11-23 tredje ledd */
    private class Arbeid(
        val timerArbeid: TimerArbeid?,
        val arbeidsevne: Prosent?,
    )

    private fun regnUtGradering(arbeidMeldeperioden: Tidslinje<Arbeid>): Tidslinje<ArbeidsGradering> {
        val arbeidstimerMeldeperioden =
            arbeidMeldeperioden.sumOf { it.verdi.timerArbeid?.antallTimer ?: BigDecimal.ZERO }

        val antallDager = BigDecimal(arbeidMeldeperioden.helePerioden().antallDager())

        // TODO: Hvordan skal vi regne ut andel arbeid hvis siste meldeperiode slutter før 14 dager.
        // Siste meldeperiode i rettighetsperioden er ikke nødvendig vis 14 dager lang.
        // Vi skalerer derfor antall timer i meldeperiode med hvor lang meldeperioden faktisk er, altså:
        // (antall timer arbeidet) / (antall timer i meldeperiode * (antall faktiske timer i meldeperioden / 14))
        // men for å bevare presisjon er formelen stokket om.
        val andelArbeid = Prosent.fraDesimal(
            minOf(
                BigDecimal.ONE,
                (arbeidstimerMeldeperioden * BigDecimal(14)).divide(ANTALL_TIMER_I_MELDEPERIODE * antallDager, 3, RoundingMode.HALF_UP)
            )
        )

        return arbeidMeldeperioden.mapValue { arbeid ->
            val fastsattArbeidsevne = arbeid.arbeidsevne ?: `0_PROSENT`
            ArbeidsGradering(
                totaltAntallTimer = arbeid.timerArbeid ?: TimerArbeid(BigDecimal.ZERO),
                andelArbeid = andelArbeid,
                fastsattArbeidsevne = fastsattArbeidsevne,
                gradering = Prosent.`100_PROSENT`.minus(
                    Prosent.max(andelArbeid, fastsattArbeidsevne)
                ),
            )
        }
    }
}