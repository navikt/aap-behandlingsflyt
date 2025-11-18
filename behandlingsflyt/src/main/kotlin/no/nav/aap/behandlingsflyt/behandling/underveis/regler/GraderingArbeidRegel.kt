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
import no.nav.aap.komponenter.tidslinje.somTidslinje
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
    class OpplysningerOmArbeid(
        /** null representerer fravær av opplysninger */
        val timerArbeid: TimerArbeid? = null,
        /** null representerer fravær av opplysninger */
        val arbeidsevne: Prosent? = null,
        val opplysningerFørstMottatt: LocalDate? = null,
        val harRett: Boolean? = null,
    ) {
        companion object {
            fun mergePrioriterHøyre(venstre: OpplysningerOmArbeid?, høyre: OpplysningerOmArbeid?) =
                OpplysningerOmArbeid(
                    timerArbeid = høyre?.timerArbeid ?: venstre?.timerArbeid,
                    arbeidsevne = høyre?.arbeidsevne ?: venstre?.arbeidsevne,
                    opplysningerFørstMottatt = listOfNotNull(
                        venstre?.opplysningerFørstMottatt,
                        høyre?.opplysningerFørstMottatt
                    ).minOrNull(),
                    harRett = høyre?.harRett ?: venstre?.harRett,
                )
        }
    }

    private fun graderingerTidslinje(
        resultat: Tidslinje<Vurdering>,
        input: UnderveisInput
    ): Tidslinje<ArbeidsGradering> {
        var opplysninger = Tidslinje(input.rettighetsperiode, OpplysningerOmArbeid())
            .outerJoin(arbeidsevnevurdering(input), OpplysningerOmArbeid::mergePrioriterHøyre)
            .outerJoin(nullTimerVedFritakFraMeldeplikt(input), OpplysningerOmArbeid::mergePrioriterHøyre)
            .outerJoin(opplysningerFraMeldekort(input), OpplysningerOmArbeid::mergePrioriterHøyre)
            .outerJoin(harRettTidslinje(resultat), OpplysningerOmArbeid::mergePrioriterHøyre)

        if (!input.ikkeAntaNullTimerArbeidetFeature) {
            if (skalAntaTimerArbeidet(resultat, opplysninger)) {
                // anta null timer arbeidet hvis medlemmet har gitt alle opplysninger
                opplysninger = Tidslinje(
                    input.rettighetsperiode,
                    OpplysningerOmArbeid(timerArbeid = TimerArbeid(BigDecimal.ZERO))
                )
                    .outerJoin(opplysninger, OpplysningerOmArbeid::mergePrioriterHøyre)
            }
        }

        return groupByMeldeperiode(resultat, opplysninger)
            .flatMap { meldeperiode ->
                regnUtGradering(meldeperiode.verdi)
            }
            .komprimer()
    }

    fun skalAntaTimerArbeidet(
        underveisVurderinger: Tidslinje<Vurdering>,
        opplysningerTidslinje: Tidslinje<OpplysningerOmArbeid>,
        dagensDato: LocalDate = LocalDate.now(),
    ): Boolean {

        val skalHaOpplysningerTidslinje = underveisVurderinger.mapValue { underveisVurdering ->
            /* uten rett skal vi ikke ha opplysninger */
            if (underveisVurdering.fårAapEtter == null) {
                return@mapValue false
            }

            val fastsattDag = underveisVurdering.meldeperiode().tom.plusDays(1)
            val sisteFrist = fastsattDag.plusDays(7)

            return@mapValue sisteFrist < dagensDato
        }

        val manglerOpplysninger =
            skalHaOpplysningerTidslinje.outerJoin(opplysningerTidslinje) { skalHaOpplysninger, opplysninger ->
                if (skalHaOpplysninger == true) {
                    return@outerJoin opplysninger?.timerArbeid == null
                } else {
                    return@outerJoin false
                }
            }

        return manglerOpplysninger.segmenter().none { it.verdi }
    }

    private fun harRettTidslinje(vurderinger: Tidslinje<Vurdering>): Tidslinje<OpplysningerOmArbeid> {
        return vurderinger.mapValue { vurdering ->
            OpplysningerOmArbeid(harRett = vurdering.fårAapEtter != null)
        }
    }

    private fun arbeidsevnevurdering(input: UnderveisInput): Tidslinje<OpplysningerOmArbeid> {
        return input.arbeidsevneGrunnlag.vurderinger.tidslinje().mapValue {
            OpplysningerOmArbeid(arbeidsevne = it.arbeidsevne)
        }
    }

    private fun nullTimerVedFritakFraMeldeplikt(
        input: UnderveisInput,
        dagensDato: LocalDate = LocalDate.now()
    ): Tidslinje<OpplysningerOmArbeid> =
        Tidslinje.map2(
            input.meldeperioder.somTidslinje { it },
            input.meldepliktGrunnlag.tilTidslinje()
        ) { meldeperiode, fritaksvurdering ->
            val harPassertMeldeperiodeITid = meldeperiode?.let { dagensDato >= meldeperiode.tom.plusDays(1) } ?: false
            if (fritaksvurdering?.harFritak == true && harPassertMeldeperiodeITid) {
                OpplysningerOmArbeid(
                    timerArbeid = TimerArbeid(BigDecimal.ZERO),
                    opplysningerFørstMottatt = meldeperiode.tom.plusDays(3) // Settes til samme dag som fritak-jobbkjøringstidspunktet
                )
            } else {
                OpplysningerOmArbeid()
            }
        }


    private fun opplysningerFraMeldekort(input: UnderveisInput): Tidslinje<OpplysningerOmArbeid> {
        var tidslinje = Tidslinje<OpplysningerOmArbeid>()

        for (meldekort in input.meldekort.sortedBy { it.mottattTidspunkt }) {
            tidslinje = tidslinje.outerJoin(meldekort.somTidslinje()) { tidligereOpplysninger, meldekortopplysninger ->
                /* Opplysninger fra nyeste meldekort, opplysningerFørstMottatt fra eldste meldekort */
                val timerArbeidetOpplysninger = OpplysningerOmArbeid(
                    timerArbeid = meldekortopplysninger?.let { (timerArbeidet, antallDager) ->
                        TimerArbeid(
                            timerArbeidet.antallTimer.divide(
                                BigDecimal(antallDager),
                                3,
                                RoundingMode.HALF_UP
                            )
                        )
                    },
                    opplysningerFørstMottatt = meldekort.mottattTidspunkt.toLocalDate(),
                )

                OpplysningerOmArbeid.mergePrioriterHøyre(tidligereOpplysninger, timerArbeidetOpplysninger)
            }
        }

        return tidslinje
    }

    private fun regnUtGradering(
        opplysningerOmArbeid: Tidslinje<OpplysningerOmArbeid>,
    ): Tidslinje<ArbeidsGradering> {
        val antallHverdager = opplysningerOmArbeid
            .segmenter().sumOf {
                if (it.verdi.harRett == true)
                    BigDecimal(it.periode.antallHverdager().asInt)
                else
                    BigDecimal.ZERO
            }

        if (antallHverdager == BigDecimal.ZERO || opplysningerOmArbeid.segmenter()
                .any { it.verdi.harRett == true && it.verdi.timerArbeid == null }
        ) {
            /* mangler opplysninger for hele perioden, vet derfor ikke hva som er
             * totalt antall timer.
             */
            return opplysningerOmArbeid.mapValue {
                ArbeidsGradering(
                    totaltAntallTimer = TimerArbeid(BigDecimal.ZERO),
                    andelArbeid = `0_PROSENT`,
                    fastsattArbeidsevne = it.arbeidsevne ?: `0_PROSENT`,
                    gradering = `0_PROSENT`,
                    opplysningerMottatt = null,
                )
            }
        }


        val timerArbeidet = opplysningerOmArbeid.segmenter().sumOf {
            if (it.verdi.harRett == true)
                it.verdi.timerArbeid!!.antallTimer * BigDecimal(it.periode.antallDager())
            else
                BigDecimal.ZERO
        }


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
                opplysningerMottatt =
                    opplysningerOmArbeid.segmenter().mapNotNull { it.verdi.opplysningerFørstMottatt }
                        /* Høyeste dato er datoen første dato vi hadde opplysninger for *hele* meldeperioden. */
                        .maxOrNull()
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