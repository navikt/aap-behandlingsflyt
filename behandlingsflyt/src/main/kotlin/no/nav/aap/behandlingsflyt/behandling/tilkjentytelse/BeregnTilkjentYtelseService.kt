package no.nav.aap.behandlingsflyt.behandling.tilkjentytelse

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.MeldepliktStatus
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.BarnetilleggGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.tilTidslinje
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Grunnlag
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
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.GUnit
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.komponenter.verdityper.Prosent.Companion.`66_PROSENT`
import java.time.LocalDate


class BeregnTilkjentYtelseService(
    private val fødselsdato: Fødselsdato,
    private val beregningsgrunnlag: Grunnlag?,
    private val underveisgrunnlag: UnderveisGrunnlag,
    private val barnetilleggGrunnlag: BarnetilleggGrunnlag,
    private val samordningGrunnlag: SamordningGrunnlag,
    private val samordningUføre: SamordningUføreGrunnlag?,
    private val samordningArbeidsgiver: SamordningArbeidsgiverGrunnlag?,
) {

    internal companion object {
        private const val ANTALL_ÅRLIGE_ARBEIDSDAGER = 260
    }

    fun beregnTilkjentYtelse(): Tidslinje<Tilkjent> {
        /** § 11-19 Grunnlaget for beregningen av arbeidsavklaringspenger. */
        val grunnlagsfaktor = beregningsgrunnlag?.grunnlaget() ?: GUnit(0)

        /** § 11-20 første avsnitt:
         * > Arbeidsavklaringspenger gis med 66 prosent av grunnlaget, se § 11-19.
         * > Minste årlige ytelse er 2,041 ganger grunnbeløpet.
         * > For medlem under 25 år er minste årlige ytelse 2/3 av 2,041 ganger grunnbeløpet
         */
        val årligYtelseTidslinje = aldersjusteringAvMinsteÅrligeYtelse(fødselsdato)
            .innerJoin(MINSTE_ÅRLIG_YTELSE_TIDSLINJE) { aldersjustering, minsteYtelse ->
                maxOf(
                    aldersjustering(minsteYtelse),
                    grunnlagsfaktor.multiplisert(`66_PROSENT`)
                )
            }

        val gradertÅrligYtelseTidslinje =
            underveisgrunnlag.somTidslinje().innerJoin(årligYtelseTidslinje) { underveisperiode, årligYtelse ->
                TilkjentGUnit(
                    /** § 11-20 andre avsnitt andre setning:
                     * > Dagsatsen er den årlige ytelsen delt på 260
                     */
                    dagsats = årligYtelse.dividert(ANTALL_ÅRLIGE_ARBEIDSDAGER),

                    gradering = TilkjentGradering(
                        endeligGradering = if (underveisperiode.utfall == Utfall.IKKE_OPPFYLT) {
                            Prosent.`0_PROSENT`
                        } else {
                            underveisperiode.arbeidsgradering.gradering
                        },
                        arbeidGradering = underveisperiode.arbeidsgradering.gradering,
                        institusjonGradering = underveisperiode.institusjonsoppholdReduksjon,
                        samordningGradering = Prosent.`0_PROSENT`,
                        samordningUføregradering = Prosent.`0_PROSENT`,
                        samordningArbeidsgiverGradering = Prosent.`0_PROSENT`
                    ),

                    utbetalingsdato = utledUtbetalingsdato(underveisperiode)
                )
            }

        val samordningUføreTidslinje = samordningUføre?.vurdering?.tilTidslinje().orEmpty()
        val gradertÅrligYtelseTidslinjeMedSamordningUføre =
            gradertÅrligYtelseTidslinje.leftJoin(samordningUføreTidslinje) { tilkjentGUnit, samordningUføre ->
                if (samordningUføre == null) {
                    tilkjentGUnit
                } else {
                    tilkjentGUnit.copy(
                        gradering = tilkjentGUnit.gradering.copy(
                            endeligGradering = tilkjentGUnit.gradering.endeligGradering.minus(
                                samordningUføre,
                                Prosent.`0_PROSENT`
                            ),
                            samordningUføregradering = samordningUføre
                        )
                    )
                }
            }

        val samordningTidslinje = samordningGrunnlag.samordningPerioder.map { Segment(it.periode, it) }.let(::Tidslinje)
        val gradertÅrligYtelseTidslinjeMedSamordning =
            gradertÅrligYtelseTidslinjeMedSamordningUføre.leftJoin(samordningTidslinje) { tilkjentGUnit, samordning ->
                if (samordning == null) {
                    tilkjentGUnit
                } else {
                    tilkjentGUnit.copy(
                        gradering = tilkjentGUnit.gradering.copy(
                            endeligGradering = tilkjentGUnit.gradering.endeligGradering.minus(
                                samordning.gradering,
                                Prosent.`0_PROSENT`
                            ),
                            samordningGradering = samordning.gradering,
                        )
                    )
                }
            }

        val samordningArbeidsgiverTidslinje = samordningArbeidsgiver?.vurdering?.tilTidslinje()
        val gradertÅrligYtelseTidslinjeMedArbeidgiver =
            gradertÅrligYtelseTidslinjeMedSamordning.leftJoin(samordningArbeidsgiverTidslinje.orEmpty()) { tilkjentGUnit, samordningArbeidsgiver ->
                if (samordningArbeidsgiver == null) {
                    tilkjentGUnit
                } else {
                    tilkjentGUnit.copy(
                        gradering = tilkjentGUnit.gradering.copy(
                            endeligGradering = tilkjentGUnit.gradering.endeligGradering.minus(
                                Prosent.`100_PROSENT`,
                                Prosent.`0_PROSENT`
                            ),
                            samordningArbeidsgiverGradering = Prosent.`100_PROSENT`,
                        )
                    )
                }
            }

        val gradertÅrligTilkjentYtelseBeløp =
            gradertÅrligYtelseTidslinjeMedArbeidgiver.innerJoin(Grunnbeløp.tilTidslinje()) { tilkjentGUnit, grunnbeløp ->
                val dagsats = grunnbeløp.multiplisert(tilkjentGUnit.dagsats)
                val redusertUtbetalingsgrad = reduserUtbetalingsgradVedInstitusjonsopphold(tilkjentGUnit.gradering)
                TilkjentFørBarn(
                    dagsats = dagsats,
                    gradering = redusertUtbetalingsgrad,
                    grunnlagsfaktor = tilkjentGUnit.dagsats,
                    grunnbeløp = grunnbeløp,
                    utbetalingsdato = tilkjentGUnit.utbetalingsdato,
                    samordningGradering = tilkjentGUnit.gradering.samordningGradering,
                    samordningUføreGradering = tilkjentGUnit.gradering.samordningUføregradering,
                    arbeidsGradering = tilkjentGUnit.gradering.arbeidGradering,
                    institusjonGradering = tilkjentGUnit.gradering.institusjonGradering,
                    samordningArbeidsgiverGradering = tilkjentGUnit.gradering.samordningArbeidsgiverGradering,
                )
            }

        val barnetilleggGrunnlagTidslinje = barnetilleggGrunnlag.perioder.tilTidslinje()
        val barnetilleggTidslinje =
            BARNETILLEGGSATS_TIDSLINJE.innerJoin(barnetilleggGrunnlagTidslinje) { barnetilleggsats, rettTilBarnetillegg ->
                Barnetillegg(
                    barnetillegg = barnetilleggsats.multiplisert(rettTilBarnetillegg.barnMedRettTil().size),
                    antallBarn = rettTilBarnetillegg.barnMedRettTil().size,
                    barnetilleggsats = barnetilleggsats
                )
            }

        return gradertÅrligTilkjentYtelseBeløp.leftJoin(barnetilleggTidslinje) { tilkjentFørBarn, barnetillegg ->
            val gradering = TilkjentGradering(
                samordningGradering = tilkjentFørBarn.samordningGradering,
                institusjonGradering = tilkjentFørBarn.institusjonGradering,
                arbeidGradering = tilkjentFørBarn.arbeidsGradering,
                endeligGradering = tilkjentFørBarn.gradering,
                samordningUføregradering = tilkjentFørBarn.samordningUføreGradering,
                samordningArbeidsgiverGradering = tilkjentFørBarn.samordningArbeidsgiverGradering,
            )
            Tilkjent(
                dagsats = tilkjentFørBarn.dagsats,
                gradering = gradering,
                barnetillegg = barnetillegg?.barnetillegg ?: Beløp(0),
                grunnlagsfaktor = tilkjentFørBarn.grunnlagsfaktor,
                grunnbeløp = tilkjentFørBarn.grunnbeløp,
                antallBarn = barnetillegg?.antallBarn ?: 0,
                barnetilleggsats = barnetillegg?.barnetilleggsats ?: Beløp(0),
                utbetalingsdato = tilkjentFørBarn.utbetalingsdato,
            )
        }
    }

    private fun utledUtbetalingsdato(underveisperiode: Underveisperiode): LocalDate {
        val meldeperiode = underveisperiode.meldePeriode
        val opplysningerMottatt = underveisperiode.arbeidsgradering.opplysningerMottatt

        val sisteMeldedagForMeldeperiode = meldeperiode.tom.plusDays(9)
        val førsteMeldedagForMeldeperiode = meldeperiode.tom.plusDays(1)
        val muligUtbetalingsdato = when {
            opplysningerMottatt != null -> opplysningerMottatt
            underveisperiode.meldepliktStatus == MeldepliktStatus.FRITAK -> førsteMeldedagForMeldeperiode
            else -> sisteMeldedagForMeldeperiode
        }
        val utbetalingsdato = muligUtbetalingsdato
            .coerceIn(førsteMeldedagForMeldeperiode..sisteMeldedagForMeldeperiode)
        return utbetalingsdato
    }

    private fun reduserUtbetalingsgradVedInstitusjonsopphold(tilkjentGradering: TilkjentGradering) =
        tilkjentGradering.endeligGradering.multiplisert(tilkjentGradering.institusjonGradering.komplement())

    private class TilkjentFørBarn(
        val dagsats: Beløp,
        val gradering: Prosent,
        val grunnlagsfaktor: GUnit,
        val grunnbeløp: Beløp,
        val utbetalingsdato: LocalDate,
        val samordningGradering: Prosent,
        val samordningUføreGradering: Prosent,
        val samordningArbeidsgiverGradering: Prosent,
        val arbeidsGradering: Prosent,
        val institusjonGradering: Prosent
    )

    private class Barnetillegg(
        val antallBarn: Int,
        val barnetilleggsats: Beløp,
        val barnetillegg: Beløp
    )
}
