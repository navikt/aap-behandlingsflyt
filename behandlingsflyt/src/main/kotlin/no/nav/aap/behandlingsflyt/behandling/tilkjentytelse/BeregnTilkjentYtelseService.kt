package no.nav.aap.behandlingsflyt.behandling.tilkjentytelse

import no.nav.aap.behandlingsflyt.behandling.underveis.regler.MeldepliktStatus
import no.nav.aap.behandlingsflyt.behandling.underveis.regler.UnntakFastsattMeldedag
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
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
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
    private val unleashGateway: UnleashGateway,
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

        val barnetilleggGrunnlagTidslinje = barnetilleggGrunnlag.perioder.tilTidslinje()
        val barnetilleggTidslinje =
            BARNETILLEGGSATS_TIDSLINJE.innerJoin(barnetilleggGrunnlagTidslinje) { barnetilleggsats, rettTilBarnetillegg ->
                val antallBarnMedRett = rettTilBarnetillegg.barnMedRettTil().size
                Barnetillegg(
                    barnetillegg = barnetilleggsats.multiplisert(antallBarnMedRett),
                    antallBarn = antallBarnMedRett,
                    barnetilleggsats = barnetilleggsats
                )
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

        return Tidslinje.map5(
            underveisgrunnlag.somTidslinje(),
            dagsatsTidslinje,
            graderingGrunnlagTidslinje,
            Grunnbeløp.tilTidslinje(),
            barnetilleggTidslinje,
        ) { underveisperiode, dagsatsG, graderingGrunnlag, grunnbeløp, barnetillegg ->
            if (underveisperiode == null || dagsatsG == null || graderingGrunnlag == null || grunnbeløp == null) {
                return@map5 null
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
                barnetillegg = barnetillegg?.barnetillegg ?: Beløp(0),
                antallBarn = barnetillegg?.antallBarn ?: 0,
                barnetilleggsats = barnetillegg?.barnetilleggsats ?: Beløp(0),
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

        val muligensUnntak = if (unleashGateway.isEnabled(BehandlingsflytFeature.UnntakMeldepliktDesember)) {
            UnntakFastsattMeldedag.erSpesialPeriode(meldeperiode)
        } else null

        val sisteMeldedagForMeldeperiode = meldeperiode.tom.plusDays(9)
        val førsteMeldedagForMeldeperiode = meldeperiode.tom.plusDays(1)

        val prioritertFørstedag = muligensUnntak ?: førsteMeldedagForMeldeperiode

        val muligUtbetalingsdato = when {
            opplysningerMottatt != null -> opplysningerMottatt
            underveisperiode.meldepliktStatus == MeldepliktStatus.FRITAK -> prioritertFørstedag
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
