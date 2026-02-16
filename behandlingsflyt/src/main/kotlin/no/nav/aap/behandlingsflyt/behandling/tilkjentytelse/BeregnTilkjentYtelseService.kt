package no.nav.aap.behandlingsflyt.behandling.tilkjentytelse

import no.nav.aap.behandlingsflyt.behandling.barnetillegg.RettTilBarnetillegg
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.MeldepliktStatus
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.unntakFastsattMeldedag
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.unntakFritaksUtbetalingDato
import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.BarnetilleggGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.tilTidslinje
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.SamordningGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.arbeidsgiver.SamordningArbeidsgiverGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.arbeidsgiver.tilTidslinje
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Underveisperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.Grunnbeløp
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.filterNotNull
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.GUnit
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.komponenter.verdityper.Prosent.Companion.`0_PROSENT`
import no.nav.aap.komponenter.verdityper.Prosent.Companion.`100_PROSENT`
import no.nav.aap.komponenter.verdityper.Prosent.Companion.`66_PROSENT`
import org.slf4j.LoggerFactory
import java.time.LocalDate

class TilkjentYtelseGrunnlag(
    val fødselsdato: Fødselsdato,
    val beregningsgrunnlag: GUnit?,
    val underveisgrunnlag: UnderveisGrunnlag,
    val barnetilleggGrunnlag: BarnetilleggGrunnlag,
    val samordningGrunnlag: SamordningGrunnlag,
    val samordningUføre: SamordningUføreGrunnlag?,
    val samordningArbeidsgiver: SamordningArbeidsgiverGrunnlag?,
    val minsteÅrligeYtelse: Tidslinje<GUnit> = MINSTE_ÅRLIG_YTELSE_TIDSLINJE,
) : Faktagrunnlag

class BeregnTilkjentYtelseService(private val grunnlag: TilkjentYtelseGrunnlag) {

    private val log = LoggerFactory.getLogger(javaClass)

    internal companion object {
        const val ANTALL_ÅRLIGE_ARBEIDSDAGER = 260
    }

    /** Regner ut tilkjent ytelse, per dag. Utregningen er basert på:
     * - [dagsatsen][beregnDagsatsTidslinje]
     * - [barnetillegget][utledBarnetillegg]
     * - [graderingen][gradering]
     *
     * Endelig dagsats regnes ut som:
     * ```
     * (barnetillegg × gradering) + (dagsats × gradering)
     * ```
     */
    fun beregnTilkjentYtelse(): Tidslinje<Tilkjent> {
        return Tidslinje.map6(
            grunnlag.underveisgrunnlag.somTidslinje(),
            beregnDagsatsTidslinje(),
            gradering(),
            Grunnbeløp.tilTidslinje(),
            BARNETILLEGGSATS_TIDSLINJE,
            grunnlag.barnetilleggGrunnlag.perioder.tilTidslinje(),
        ) { underveisperiode, dagsats, graderingGrunnlag, grunnbeløp, barnetilleggsats, rettTilBarnetillegg ->
            if (underveisperiode == null || dagsats == null || graderingGrunnlag == null || grunnbeløp == null) {
                return@map6 null
            }

            val (dagsatsG, minstesats) = dagsats

            val barnetillegg = utledBarnetillegg(
                underveisperiode = underveisperiode,
                barnetilleggsats = barnetilleggsats,
                rettTilBarnetillegg = rettTilBarnetillegg
            )

            val ikkeOppfylt = underveisperiode.utfall == Utfall.IKKE_OPPFYLT

            Tilkjent(
                dagsats = grunnbeløp.multiplisert(dagsatsG),
                gradering = if (ikkeOppfylt) `0_PROSENT` else graderingGrunnlag.gradering(),
                graderingGrunnlag = if (ikkeOppfylt) graderingGrunnlag.copy(institusjonGradering = `0_PROSENT`) else graderingGrunnlag,
                barnetillegg = barnetillegg.barnetillegg,
                antallBarn = barnetillegg.antallBarn,
                barnetilleggsats = barnetillegg.barnetilleggsats,
                grunnlagsfaktor = dagsatsG,
                grunnbeløp = grunnbeløp,
                utbetalingsdato = utledUtbetalingsdato(underveisperiode),
                minsteSats = minstesats,
                redusertDagsats = null
            ).run { copy(redusertDagsats = redusertDagsats()) }
        }
            .filterNotNull()
    }

    /** Regner ut reduksjon av AAP, som en prosent, for en gitt dag.
     * Hvis resultatet er 0%, så betyr det at ingen AAP skal utbetales den dagen.
     * Hvis resultatet er 100%, så betyr det at full AAP skal utbetales den dagen.
     * Hvis resultatet er 70%, så betyr det at 70% av full AAP skal utbetales den dagen.
     *
     * Følgende legges til grunn for utregningen:
     * - [Gradering basert på arbeid][no.nav.aap.behandlingsflyt.behandling.underveis.regler.GraderingArbeidRegel] (prosent, heltall)
     * - Samordning med uføre (prosent, heltall)
     * - Samordning (annet) (prosent, heltall)
     * - [Samordning ytelser fra arbeidsgiver][no.nav.aap.behandlingsflyt.forretningsflyt.steg.SamordningArbeidsgiverSteg] (prosent, heltall)
     * - [Reduksjon på grunn av ikke oppfylt meldeplikt][no.nav.aap.behandlingsflyt.behandling.underveis.regler.MeldepliktRegel] (prosent, heltall)
     * - [Gradering på grunn av opphold på helseinstitusjon][no.nav.aap.behandlingsflyt.behandling.underveis.regler.InstitusjonRegel] (prosent, heltall)
     *
     * Utregningen gjøres med heltall mellom 0 og 100 (prosenter). Resultatet og delresultater kan aldri bli mindre enn 0%.
     *
     * ```
     *  (
     *         100
     *       – graderingArbeidReduksjon
     *       – uføreReduksjon
     *       – annenSamordningReduksjon
     *       – ytelserArbeidsgiverReduksjon
     *       – meldepliktReduksjon
     *   )
     *      × institusjonsgradering
     * ```
     */
    fun GraderingGrunnlag.gradering(): Prosent {
        return `100_PROSENT`
            .minus(arbeidGradering.komplement())
            .minus(samordningUføregradering)
            .minus(samordningGradering)
            .minus(samordningArbeidsgiverGradering)
            .minus(meldepliktGradering)
            .multiplisert(institusjonGradering.komplement())
    }

    private fun gradering(): Tidslinje<GraderingGrunnlag> = Tidslinje.map4(
        grunnlag.underveisgrunnlag.somTidslinje(),
        grunnlag.samordningUføre?.vurdering?.tilTidslinje().orEmpty(),
        grunnlag.samordningGrunnlag.samordningPerioder.map { Segment(it.periode, it) }.let(::Tidslinje),
        grunnlag.samordningArbeidsgiver?.vurdering?.tilTidslinje().orEmpty(),
    ) { underveisperiode, samordningUføre, samordning, samordningArbeidsgiver ->
        if (underveisperiode == null) {
            return@map4 null
        }

        GraderingGrunnlag(
            arbeidGradering = underveisperiode.arbeidsgradering.gradering,
            institusjonGradering = underveisperiode.institusjonsoppholdReduksjon,
            samordningGradering = samordning?.gradering ?: `0_PROSENT`,
            samordningUføregradering = samordningUføre ?: `0_PROSENT`,
            samordningArbeidsgiverGradering = if (samordningArbeidsgiver == null) `0_PROSENT` else `100_PROSENT`,
            meldepliktGradering = underveisperiode.meldepliktGradering ?: `0_PROSENT`,
        )
    }
        .filterNotNull()

    private data class Dagsats(
        val dagsats: GUnit,
        val minstesats: Minstesats
    )

    /** Utregning av dagsats, beregnet per dag.
     * Følgende legges til grunn:
     * - [Grunnlaget fra § 11-19][no.nav.aap.behandlingsflyt.behandling.beregning.BeregningService.beregnGrunnlag] i G (10 desimaler)
     * - [Utgangspunktet for minste årlig ytelse, definert per dag][MINSTE_ÅRLIG_YTELSE_TIDSLINJE] i G (10 desimaler)
     * - [Aldersjustering av minstesats, definert per dag][aldersjusteringAvMinsteÅrligeYtelse] transformasjon av G til G (10 desimaler)
     *
     * Formelen, som regnes ut for hver dag, er:
     * ```
     * (aldersjustert(minstesats) × grunnlagsfaktor × 0.66) / 260
     * ```
     *
     * @return Minstesatsjustert dagsats, per dag, i G (10 desimaler)
     */
    private fun beregnDagsatsTidslinje(): Tidslinje<Dagsats> {
        /** § 11-19 Grunnlaget for beregningen av arbeidsavklaringspenger. */
        val grunnlagsfaktor = grunnlag.beregningsgrunnlag ?: GUnit(0)

        /** § 11-20 første avsnitt:
         * > Arbeidsavklaringspenger gis med 66 prosent av grunnlaget, se § 11-19.
         * > Minste årlige ytelse er 2,041 ganger grunnbeløpet.
         * > For medlem under 25 år er minste årlige ytelse 2/3 av 2,041 ganger grunnbeløpet
         */
        val årligYtelseFørMax = grunnlagsfaktor.multiplisert(`66_PROSENT`)

        return aldersjusteringAvMinsteÅrligeYtelse(grunnlag.fødselsdato)
            .innerJoin(grunnlag.minsteÅrligeYtelse) { aldersjustering, minsteYtelse ->
                /** § 11-20 første avsnitt:
                 * > Arbeidsavklaringspenger gis med 66 prosent av grunnlaget, se § 11-19.
                 * > Minste årlige ytelse er 2,041 ganger grunnbeløpet.
                 * > For medlem under 25 år er minste årlige ytelse 2/3 av 2,041 ganger grunnbeløpet
                 */
                val årligYtelse = aldersjustering(minsteYtelse, årligYtelseFørMax)

                /** § 11-20 andre avsnitt andre setning:
                 * > Dagsatsen er den årlige ytelsen delt på 260
                 */
                Dagsats(årligYtelse.årligYtelse.dividert(ANTALL_ÅRLIGE_ARBEIDSDAGER), årligYtelse.minstesats)
            }
    }

    private fun utledBarnetillegg(
        underveisperiode: Underveisperiode,
        barnetilleggsats: Beløp?,
        rettTilBarnetillegg: RettTilBarnetillegg?
    ): Barnetillegg {
        val antallBarnMedRett = rettTilBarnetillegg?.barnMedRettTil()?.size ?: 0
        val harRettTilBarnetillegg = barnetilleggsats != null && underveisperiode.utfall != Utfall.IKKE_OPPFYLT

        return when {
            harRettTilBarnetillegg -> Barnetillegg(
                barnetillegg = barnetilleggsats.multiplisert(antallBarnMedRett),
                antallBarn = antallBarnMedRett,
                barnetilleggsats = barnetilleggsats
            )

            else -> Barnetillegg(
                barnetillegg = Beløp(0),
                antallBarn = 0,
                barnetilleggsats = Beløp(0)
            )
        }
    }

    private fun utledUtbetalingsdato(underveisperiode: Underveisperiode): LocalDate {
        val meldeperiode = underveisperiode.meldePeriode
        val opplysningerMottatt = underveisperiode.arbeidsgradering.opplysningerMottatt

        val unntakFastsattMeldedag =
        // `meldeperiode` svarer til perioden det ble skrevet meldekort for (på dato `opplysningerMottatt`).
            // For å finne unntakts-meldepliktperiode, må vi flytte denne to uker fram.
            unntakFastsattMeldedag[meldeperiode.flytt(14).fom]

        val sisteMeldedagForMeldeperiode = meldeperiode.tom.plusDays(9)
        val førsteMeldedagForMeldeperiode = meldeperiode.tom.plusDays(1)

        val prioritertFørstedag = unntakFastsattMeldedag ?: førsteMeldedagForMeldeperiode

        // Hvis fritak fra meldeplikt, betal ut så tidlig som mulig.
        // Ellers, betal ut etter dato for levert meldekort.
        // Fallback til siste meldedag for meldeperiode.
        // kanskje denne burde være min, ikke when
        val muligUtbetalingsdato = when {
            opplysningerMottatt != null -> opplysningerMottatt
            underveisperiode.meldepliktStatus == MeldepliktStatus.FRITAK -> {
                log.info("Traff sjekk for meldepliktstatus == FRITAK.")
                unntakFritaksUtbetalingDato[førsteMeldedagForMeldeperiode]
                    ?: førsteMeldedagForMeldeperiode
            }

            else -> sisteMeldedagForMeldeperiode
        }
        val utbetalingsdato = muligUtbetalingsdato
            .coerceIn(prioritertFørstedag..sisteMeldedagForMeldeperiode)
        return utbetalingsdato
    }

    private class Barnetillegg(
        val antallBarn: Int,
        val barnetilleggsats: Beløp,
        val barnetillegg: Beløp
    )
}
