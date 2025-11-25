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
import no.nav.aap.komponenter.tidslinje.filterNotNull
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.GUnit
import no.nav.aap.komponenter.verdityper.Prosent.Companion.`0_PROSENT`
import no.nav.aap.komponenter.verdityper.Prosent.Companion.`100_PROSENT`
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

        val dagsatsTidslinje = aldersjusteringAvMinsteÅrligeYtelse(fødselsdato)
            .innerJoin(MINSTE_ÅRLIG_YTELSE_TIDSLINJE) { aldersjustering, minsteYtelse ->
                /** § 11-20 første avsnitt:
                 * > Arbeidsavklaringspenger gis med 66 prosent av grunnlaget, se § 11-19.
                 * > Minste årlige ytelse er 2,041 ganger grunnbeløpet.
                 * > For medlem under 25 år er minste årlige ytelse 2/3 av 2,041 ganger grunnbeløpet
                 */
                val årligYtelse = maxOf(
                    aldersjustering(minsteYtelse),
                    grunnlagsfaktor.multiplisert(`66_PROSENT`)
                )

                /** § 11-20 andre avsnitt andre setning:
                 * > Dagsatsen er den årlige ytelsen delt på 260
                 */
                årligYtelse.dividert(ANTALL_ÅRLIGE_ARBEIDSDAGER)
            }

            val graderingGrunnlagTidslinje = Tidslinje.map4(
                underveisgrunnlag.somTidslinje(),
                samordningUføre?.vurdering?.tilTidslinje().orEmpty(),
                samordningGrunnlag.samordningPerioder.map { Segment(it.periode, it) }.let(::Tidslinje),
                samordningArbeidsgiver?.vurdering?.tilTidslinje().orEmpty(),
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
            )
        }
            .filterNotNull()

        return Tidslinje.map6(
            underveisgrunnlag.somTidslinje(),
            dagsatsTidslinje,
            graderingGrunnlagTidslinje,
            Grunnbeløp.tilTidslinje(),
            BARNETILLEGGSATS_TIDSLINJE,
            barnetilleggGrunnlag.perioder.tilTidslinje(),
        ) { underveisperiode, dagsatsG, graderingGrunnlag, grunnbeløp, barnetilleggsats, rettTilBarnetillegg ->
            if (underveisperiode == null || dagsatsG == null || graderingGrunnlag == null || grunnbeløp == null) {
                return@map6 null
            }

            val antallBarnMedRett = rettTilBarnetillegg?.barnMedRettTil()?.size ?: 0
            val harRettTilBarnetillegg = barnetilleggsats != null && underveisperiode.utfall != Utfall.IKKE_OPPFYLT

            val barnetillegg = if (harRettTilBarnetillegg) {
                Barnetillegg(
                    barnetillegg = barnetilleggsats.multiplisert(antallBarnMedRett),
                    antallBarn = antallBarnMedRett,
                    barnetilleggsats = barnetilleggsats
                )
            } else {
                Barnetillegg(
                    barnetillegg = Beløp(0),
                    antallBarn = 0,
                    barnetilleggsats = Beløp(0)
                )
            }

            Tilkjent(
                dagsats = grunnbeløp.multiplisert(dagsatsG),
                gradering = graderingGrunnlag.run {
                    `100_PROSENT`
                        .minus(if (underveisperiode.utfall == Utfall.IKKE_OPPFYLT) `100_PROSENT` else `0_PROSENT`)
                        .minus(arbeidGradering.komplement())
                        .minus(samordningUføregradering)
                        .minus(samordningGradering)
                        .minus(samordningArbeidsgiverGradering)
                        .multiplisert(institusjonGradering.komplement())
                },
                graderingGrunnlag = graderingGrunnlag,
                barnetillegg = barnetillegg.barnetillegg,
                antallBarn = barnetillegg.antallBarn,
                barnetilleggsats = barnetillegg.barnetilleggsats,
                grunnlagsfaktor = dagsatsG,
                grunnbeløp = grunnbeløp,
                utbetalingsdato = utledUtbetalingsdato(underveisperiode),
            )
        }
            .filterNotNull()
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

    private class Barnetillegg(
        val antallBarn: Int,
        val barnetilleggsats: Beløp,
        val barnetillegg: Beløp
    )
}
