package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.Hverdager.Companion.antallHverdager
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.UtledMeldeperiodeRegel.Companion.groupByMeldeperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.ArbeidsGradering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Innvilgelsesårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.ArbeidsevneVurdering.Companion.tidslinje
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.komponenter.verdityper.Prosent.Companion.`0_PROSENT`
import no.nav.aap.komponenter.verdityper.TimerArbeid
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

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

// § 11-23 tredje ledd
private const val ANTALL_TIMER_I_ARBEIDSUKE = 37.5

private val HVERDAGER_I_FULL_MELDEPERIODE = BigDecimal(10)

private val ANTALL_TIMER_I_MELDEPERIODE =
    BigDecimal(ANTALL_TIMER_I_ARBEIDSUKE).multiply(BigDecimal.TWO)

// § 11-23 fjerde ledd
private const val HØYESTE_GRADERING_NORMAL = 60

// § 11-23 fjerde ledd
private const val HØYESTE_GRADERING_YRKESSKADE = 70

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

    /** § 11-23 tredje ledd */
    private class OpplysningerOmArbeid(
        /** null representerer fravær av opplysninger */
        val timerArbeid: TimerArbeid?,
        /** null representerer fravær av opplysninger */
        val arbeidsevne: Prosent?,
    ) {
        companion object {
            fun mergePrioriterHøyre(venstre: OpplysningerOmArbeid?, høyre: OpplysningerOmArbeid?) =
                OpplysningerOmArbeid(
                    timerArbeid = høyre?.timerArbeid ?: venstre?.timerArbeid,
                    arbeidsevne = høyre?.arbeidsevne ?: venstre?.arbeidsevne,
                )
        }
    }

    private fun graderingerTidslinje(
        resultat: Tidslinje<Vurdering>,
        input: UnderveisInput
    ): Tidslinje<ArbeidsGradering> {
        var opplysninger = Tidslinje(input.rettighetsperiode, OpplysningerOmArbeid(null, null))
            .outerJoin(arbeidsevnevurdering(input), OpplysningerOmArbeid::mergePrioriterHøyre)
            .outerJoin(nullTimerVedFritakFraMeldeplikt(input), OpplysningerOmArbeid::mergePrioriterHøyre)
            .outerJoin(opplysningerFraMeldekort(input), OpplysningerOmArbeid::mergePrioriterHøyre)

        val dagensDato = LocalDate.now()
        val sisteMeldeperiodeSomSkalHaOpplysninger = resultat.asSequence()
            .map { it.verdi.meldeperiode() }
            .lastOrNull { it.tom.plusDays(8) <= dagensDato }
        val harGittOpplysningerFramTilNå = sisteMeldeperiodeSomSkalHaOpplysninger == null || opplysninger
            .takeWhile { it.periode.fom <= sisteMeldeperiodeSomSkalHaOpplysninger.fom }
            .let {
                it.isNotEmpty() && it.all { it.verdi.timerArbeid != null }
            }

        if (harGittOpplysningerFramTilNå) {
            // anta null timer arbeidet hvis medlemmet har gitt alle opplysninger
            opplysninger = Tidslinje(input.rettighetsperiode, OpplysningerOmArbeid(timerArbeid = TimerArbeid(BigDecimal.ZERO), null))
                .outerJoin(opplysninger, OpplysningerOmArbeid::mergePrioriterHøyre)
        }

        return groupByMeldeperiode(resultat, opplysninger)
            .flatMap { meldeperiode ->
                regnUtGradering(meldeperiode.periode, meldeperiode.verdi)
            }
            .komprimer()
    }

    private fun arbeidsevnevurdering(input: UnderveisInput): Tidslinje<OpplysningerOmArbeid> {
        return input.arbeidsevneGrunnlag.vurderinger.tidslinje().mapValue {
            OpplysningerOmArbeid(
                timerArbeid = null,
                arbeidsevne = it.arbeidsevne
            )
        }
    }

    private fun nullTimerVedFritakFraMeldeplikt(input: UnderveisInput): Tidslinje<OpplysningerOmArbeid> =
        input.meldepliktGrunnlag.tilTidslinje().mapValue {
            if (it.harFritak) {
                OpplysningerOmArbeid(
                    timerArbeid = TimerArbeid(BigDecimal.ZERO),
                    arbeidsevne = null,
                )
            } else {
                OpplysningerOmArbeid(timerArbeid = null, arbeidsevne = null)
            }
        }

    private fun opplysningerFraMeldekort(input: UnderveisInput): Tidslinje<OpplysningerOmArbeid> {
        val innsendt = input.innsendingsTidspunkt.map { it.value to it.key }.toMap()
        var tidslinje = Tidslinje<OpplysningerOmArbeid>()
        for (meldekort in input.meldekort.sortedBy { innsendt[it.journalpostId] }) {
            tidslinje = tidslinje.outerJoin(meldekort.somTidslinje()) { tidligereOpplysnigner, meldekortopplysninger ->
                OpplysningerOmArbeid.mergePrioriterHøyre(
                    tidligereOpplysnigner,
                    OpplysningerOmArbeid(
                        timerArbeid = meldekortopplysninger?.let { (timerArbeidet, antallDager) ->
                            TimerArbeid(
                                timerArbeidet.antallTimer.divide(
                                    BigDecimal(antallDager),
                                    3,
                                    RoundingMode.HALF_UP
                                )
                            )
                        },
                        arbeidsevne = null,
                    )
                )
            }
        }
        return tidslinje
    }

    private fun regnUtGradering(
        periode: Periode,
        opplysningerOmArbeid: Tidslinje<OpplysningerOmArbeid>
    ): Tidslinje<ArbeidsGradering> {
        require(opplysningerOmArbeid.helePerioden() == periode)
        require(opplysningerOmArbeid.erSammenhengende())

        if (opplysningerOmArbeid.any { it.verdi.timerArbeid == null} ) {
            /* mangler opplysninger for hele perioden, vet derfor ikke hva som er
             * totalt antall timer.
             */
            return opplysningerOmArbeid.mapValue {
                ArbeidsGradering(
                    totaltAntallTimer = TimerArbeid(BigDecimal.ZERO),
                    andelArbeid = `0_PROSENT`,
                    fastsattArbeidsevne = it.arbeidsevne ?: `0_PROSENT`,
                    gradering = `0_PROSENT`,
                )
            }
        }


        val timerArbeidet = opplysningerOmArbeid.sumOf {
            (it.verdi.timerArbeid?.antallTimer ?: BigDecimal.ZERO) * BigDecimal(it.periode.antallDager())
        }
        val antallHverdager = BigDecimal(periode.antallHverdager().asInt)

        // En meldeperiode har ikke nødvendigvis 10 hverdager, f.eks. ved start og stopp.
        // Vi skalerer derfor antall timer i meldeperiode med hvor lang meldeperioden faktisk er, altså:
        // (antall timer arbeidet) / (antall timer i meldeperiode * (antall faktiske timer i meldeperioden / 10))
        // men for å bevare presisjon er formelen stokket om.
        val andelArbeid = Prosent.fraDesimal(
            minOf(
                BigDecimal.ONE,
                (timerArbeidet * HVERDAGER_I_FULL_MELDEPERIODE).divide(
                    ANTALL_TIMER_I_MELDEPERIODE * antallHverdager,
                    3,
                    RoundingMode.HALF_UP
                )
            )
        )

        return opplysningerOmArbeid.mapValue { arbeid ->
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

    private fun graderingsgrenseverdier(
        input: UnderveisInput,
        resultat: Tidslinje<Vurdering>
    ): Tidslinje<Prosent> {
        val opptrappingTidslinje =
            Tidslinje(input.opptrappingPerioder.map { Segment(it, Prosent(HØYESTE_GRADERING_OPPTRAPPING)) })

        val harYrkesskade = input.vilkårsresultat.finnVilkår(Vilkårtype.SYKDOMSVILKÅRET).vilkårsperioder()
            .any { it.utfall == Utfall.OPPFYLT && it.innvilgelsesårsak == Innvilgelsesårsak.YRKESSKADE_ÅRSAKSSAMMENHENG }

        val øvreGrenseNormalt = if (harYrkesskade) {
            Prosent(HØYESTE_GRADERING_YRKESSKADE)
        } else {
            Prosent(HØYESTE_GRADERING_NORMAL)
        }
        return resultat.mapValue { øvreGrenseNormalt }
            .kombiner(opptrappingTidslinje, StandardSammenslåere.prioriterHøyreSideCrossJoin())
    }
}