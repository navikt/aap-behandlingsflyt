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
import no.nav.aap.komponenter.verdityper.Prosent.Companion.`0_PROSENT`
import no.nav.aap.komponenter.verdityper.Prosent.Companion.`100_PROSENT`
import no.nav.aap.komponenter.verdityper.Prosent.Companion.`66_PROSENT`
import java.time.LocalDate

class TilkjentYtelseGrunnlag(
    val fødselsdato: Fødselsdato,
    val beregningsgrunnlag: GUnit?,
    val underveisgrunnlag: UnderveisGrunnlag,
    val barnetilleggGrunnlag: BarnetilleggGrunnlag,
    val samordningGrunnlag: SamordningGrunnlag,
    val samordningUføre: SamordningUføreGrunnlag?,
    val samordningArbeidsgiver: SamordningArbeidsgiverGrunnlag?,
    val unntakMeldepliktDesemberEnabled: Boolean = false
) : Faktagrunnlag

class BeregnTilkjentYtelseService(val grunnlag: TilkjentYtelseGrunnlag) {

    internal companion object {
        private const val ANTALL_ÅRLIGE_ARBEIDSDAGER = 260
    }

    fun beregnTilkjentYtelse(): Tidslinje<Tilkjent> {
        /** § 11-19 Grunnlaget for beregningen av arbeidsavklaringspenger. */
        val grunnlagsfaktor = grunnlag.beregningsgrunnlag ?: GUnit(0)

        val dagsatsTidslinje = aldersjusteringAvMinsteÅrligeYtelse(grunnlag.fødselsdato)
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
            )
        }
            .filterNotNull()

        return Tidslinje.map6(
            grunnlag.underveisgrunnlag.somTidslinje(),
            dagsatsTidslinje,
            graderingGrunnlagTidslinje,
            Grunnbeløp.tilTidslinje(),
            BARNETILLEGGSATS_TIDSLINJE,
            grunnlag.barnetilleggGrunnlag.perioder.tilTidslinje(),
        ) { underveisperiode, dagsatsG, graderingGrunnlag, grunnbeløp, barnetilleggsats, rettTilBarnetillegg ->
            if (underveisperiode == null || dagsatsG == null || graderingGrunnlag == null || grunnbeløp == null) {
                return@map6 null
            }

            val barnetillegg = utledBarnetillegg(
                underveisperiode = underveisperiode,
                barnetilleggsats = barnetilleggsats,
                rettTilBarnetillegg = rettTilBarnetillegg
            )

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
                utbetalingsdato = utledUtbetalingsdato(underveisperiode, grunnlag.unntakMeldepliktDesemberEnabled),
            )
        }
            .filterNotNull()
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

    private fun utledUtbetalingsdato(underveisperiode: Underveisperiode, unntakMeldepliktDesemberEnabled: Boolean): LocalDate {
        val meldeperiode = underveisperiode.meldePeriode
        val opplysningerMottatt = underveisperiode.arbeidsgradering.opplysningerMottatt

        val unntakFastsattMeldedag = if (unntakMeldepliktDesemberEnabled) {
            // `meldeperiode` svarer til perioden det ble skrevet meldekort for (på dato `opplysningerMottatt`).
            // For å finne unntakts-meldepliktperiode, må vi flytte denne to uker fram.
            unntakFastsattMeldedag[meldeperiode.flytt(14).fom]
        } else null

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
