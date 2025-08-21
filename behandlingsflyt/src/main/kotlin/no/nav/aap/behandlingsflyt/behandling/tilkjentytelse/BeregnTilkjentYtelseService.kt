package no.nav.aap.behandlingsflyt.behandling.tilkjentytelse

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.BarnetilleggGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.tilTidslinje
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Grunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.SamordningGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.arbeidsgiver.SamordningArbeidsgiverGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.arbeidsgiver.tilTidslinje
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.Grunnbeløp
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.komponenter.tidslinje.JoinStyle
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.GUnit
import no.nav.aap.komponenter.verdityper.Prosent
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

        internal object AldersjusteringAvMinsteÅrligYtelse :
            JoinStyle<AlderStrategi, GUnit, GUnit> by JoinStyle.INNER_JOIN(
                { periode: Periode, venstre, høyre ->
                    val minsteÅrligYtelse = høyre.verdi
                    val aldersfunksjon = venstre.verdi
                    Segment(periode, aldersfunksjon.aldersjustering(minsteÅrligYtelse))
                })
    }

    fun beregnTilkjentYtelse(): Tidslinje<Tilkjent> {
        val minsteÅrligYtelseAlderStrategiTidslinje = MinsteÅrligYtelseAlderTidslinje(fødselsdato).tilTidslinje()
        val underveisTidslinje = Tidslinje(underveisgrunnlag.perioder.map { Segment(it.periode, it) })
        val grunnlagsfaktor = beregningsgrunnlag?.grunnlaget()
        val barnetilleggGrunnlagTidslinje = barnetilleggGrunnlag.perioder.tilTidslinje()
        val utgangspunktForÅrligYtelse = grunnlagsfaktor?.multiplisert(Prosent.`66_PROSENT`) ?: GUnit(0)

        val minsteÅrligYtelseMedAlderTidslinje = minsteÅrligYtelseAlderStrategiTidslinje.kombiner(
            MINSTE_ÅRLIG_YTELSE_TIDSLINJE,
            AldersjusteringAvMinsteÅrligYtelse
        )

        val årligYtelseTidslinje = minsteÅrligYtelseMedAlderTidslinje.mapValue { minsteÅrligYtelse ->
            maxOf(minsteÅrligYtelse, utgangspunktForÅrligYtelse)
        }

        val samordningTidslinje = samordningGrunnlag.samordningPerioder.map { Segment(it.periode, it) }.let(::Tidslinje)
        val samordningUføreTidslinje = samordningUføre?.vurdering?.tilTidslinje()
        val samordningArbeidsgiverTidslinje = samordningArbeidsgiver?.vurdering?.tilTidslinje()

        val gradertÅrligYtelseTidslinje = underveisTidslinje.kombiner(
            årligYtelseTidslinje, JoinStyle.INNER_JOIN { periode, venstre, høyre ->
                val dagsats = høyre.verdi.dividert(ANTALL_ÅRLIGE_ARBEIDSDAGER)

                val utbetalingsgrad = if (venstre.verdi.utfall == Utfall.IKKE_OPPFYLT) {
                    Prosent.`0_PROSENT`
                } else {
                    venstre.verdi.arbeidsgradering.gradering
                }

                val meldeperiode = venstre.verdi.meldePeriode
                val utbetalingsdato =
                    (venstre.verdi.arbeidsgradering.opplysningerMottatt ?: meldeperiode.tom.plusDays(9))
                        .coerceIn(meldeperiode.tom.plusDays(1)..meldeperiode.tom.plusDays(9))

                Segment(
                    periode,
                    TilkjentGUnit(
                        dagsats = dagsats,
                        gradering = TilkjentGradering(
                            endeligGradering = utbetalingsgrad,
                            arbeidGradering = venstre.verdi.arbeidsgradering.gradering,
                            institusjonGradering = venstre.verdi.institusjonsoppholdReduksjon,
                            samordningGradering = Prosent.`0_PROSENT`,
                            samordningUføregradering = Prosent.`0_PROSENT`,
                            samordningArbeidsgiverGradering = Prosent.`0_PROSENT`
                        ),
                        utbetalingsdato = utbetalingsdato
                    )
                )
            })

        val gradertÅrligYtelseTidslinjeMedSamordningUføre =
            samordningUføreTidslinje?.let {
                gradertÅrligYtelseTidslinje.kombiner(it, JoinStyle.LEFT_JOIN { periode, venstre, høyre ->
                    if (høyre == null) {
                        venstre
                    } else {
                        val tilkjentGUnit = venstre.verdi
                        val nyGradering =
                            tilkjentGUnit.copy(
                                gradering = tilkjentGUnit.gradering.copy(
                                    endeligGradering = tilkjentGUnit.gradering.endeligGradering.minus(
                                        høyre.verdi,
                                        Prosent.`0_PROSENT`
                                    ),
                                    samordningUføregradering = høyre.verdi
                                )
                            )
                        Segment(periode, nyGradering)

                    }
                })
            } ?: gradertÅrligYtelseTidslinje


        val gradertÅrligYtelseTidslinjeMedSamordning =
            gradertÅrligYtelseTidslinjeMedSamordningUføre.kombiner(
                samordningTidslinje,
                JoinStyle.LEFT_JOIN { periode, venstre, høyre ->
                    if (høyre == null) {
                        venstre
                    } else {
                        val tilkjentGUnit = venstre.verdi
                        val nyGradering =
                            tilkjentGUnit.copy(
                                gradering = tilkjentGUnit.gradering.copy(
                                    endeligGradering = tilkjentGUnit.gradering.endeligGradering.minus(
                                        høyre.verdi.gradering,
                                        Prosent.`0_PROSENT`
                                    ),
                                    samordningGradering = høyre.verdi.gradering,
                                )
                            )
                        Segment(periode, nyGradering)
                    }
                })

        val gradertÅrligYtelseTidslinjeMedArbeidgiver =
            gradertÅrligYtelseTidslinjeMedSamordning.kombiner(
                samordningArbeidsgiverTidslinje ?: Tidslinje(),
                JoinStyle.LEFT_JOIN { periode, venstre, høyre ->
                    if (høyre == null) {
                        venstre
                    } else {
                        val tilkjentGUnit = venstre.verdi
                        val nyGradering =
                            tilkjentGUnit.copy(
                                gradering = tilkjentGUnit.gradering.copy(
                                    endeligGradering = tilkjentGUnit.gradering.endeligGradering.minus(
                                        Prosent.`100_PROSENT`,
                                        Prosent.`0_PROSENT`
                                    ),
                                    samordningArbeidsgiverGradering = Prosent.`100_PROSENT`,
                                )
                            )
                        Segment(periode, nyGradering)
                    }
                })

        val gradertÅrligTilkjentYtelseBeløp = gradertÅrligYtelseTidslinjeMedArbeidgiver.kombiner(
            Grunnbeløp.tilTidslinje(), JoinStyle.INNER_JOIN { periode, venstre, grunnbeløp ->
                val dagsats = grunnbeløp.verdi.multiplisert(venstre.verdi.dagsats)
                val redusertUtbetalingsgrad = reduserUtbetalingsgradVedInstitusjonsopphold(venstre.verdi.gradering)
                Segment(
                    periode, TilkjentFørBarn(
                        dagsats = dagsats,
                        gradering = redusertUtbetalingsgrad,
                        grunnlagsfaktor = venstre.verdi.dagsats,
                        grunnbeløp = grunnbeløp.verdi,
                        utbetalingsdato = venstre.verdi.utbetalingsdato,
                        samordningGradering = venstre.verdi.gradering.samordningGradering,
                        samordningUføreGradering = venstre.verdi.gradering.samordningUføregradering,
                        arbeidsGradering = venstre.verdi.gradering.arbeidGradering,
                        institusjonGradering = venstre.verdi.gradering.institusjonGradering,
                        samordningArbeidsgiverGradering = venstre.verdi.gradering.samordningArbeidsgiverGradering,
                    )
                )
            })

        val barnetilleggTidslinje = BARNETILLEGGSATS_TIDSLINJE.kombiner(
            barnetilleggGrunnlagTidslinje,
            JoinStyle.INNER_JOIN { periode, venstre, høyre ->
                Segment(
                    periode, Barnetillegg(
                        barnetillegg = venstre.verdi.multiplisert(høyre.verdi.barnMedRettTil().size),
                        antallBarn = høyre.verdi.barnMedRettTil().size,
                        barnetilleggsats = venstre.verdi
                    )
                )
            })

        return gradertÅrligTilkjentYtelseBeløp.kombiner(
            barnetilleggTidslinje,
            JoinStyle.LEFT_JOIN { periode, venstre, høyre ->
                val gradering = TilkjentGradering(
                    samordningGradering = venstre.verdi.samordningGradering,
                    institusjonGradering = venstre.verdi.institusjonGradering,
                    arbeidGradering = venstre.verdi.arbeidsGradering,
                    endeligGradering = venstre.verdi.gradering,
                    samordningUføregradering = venstre.verdi.samordningUføreGradering,
                    samordningArbeidsgiverGradering = venstre.verdi.samordningArbeidsgiverGradering,
                )
                Segment(
                    periode, Tilkjent(
                        dagsats = venstre.verdi.dagsats,
                        gradering = gradering,
                        barnetillegg = høyre?.verdi?.barnetillegg ?: Beløp(0),
                        grunnlagsfaktor = venstre.verdi.grunnlagsfaktor,
                        grunnbeløp = venstre.verdi.grunnbeløp,
                        antallBarn = høyre?.verdi?.antallBarn ?: 0,
                        barnetilleggsats = høyre?.verdi?.barnetilleggsats ?: Beløp(0),
                        utbetalingsdato = venstre.verdi.utbetalingsdato,
                    )
                )
            })
    }

    private fun reduserUtbetalingsgradVedInstitusjonsopphold(tilkjentGradering: TilkjentGradering) =
        tilkjentGradering.institusjonGradering?.let {
            tilkjentGradering.endeligGradering.multiplisert(it.komplement())
        } ?: tilkjentGradering.endeligGradering

    private class TilkjentFørBarn(
        val dagsats: Beløp,
        val gradering: Prosent,
        val grunnlagsfaktor: GUnit,
        val grunnbeløp: Beløp,
        val utbetalingsdato: LocalDate,
        val samordningGradering: Prosent?,
        val samordningUføreGradering: Prosent?,
        val samordningArbeidsgiverGradering: Prosent?,
        val arbeidsGradering: Prosent?,
        val institusjonGradering: Prosent?
    )

    private class Barnetillegg(
        val antallBarn: Int,
        val barnetilleggsats: Beløp,
        val barnetillegg: Beløp
    )
}
