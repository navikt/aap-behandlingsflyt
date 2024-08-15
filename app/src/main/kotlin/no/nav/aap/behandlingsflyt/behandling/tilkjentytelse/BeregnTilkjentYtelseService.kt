package no.nav.aap.behandlingsflyt.behandling.tilkjentytelse

import no.nav.aap.behandlingsflyt.behandling.barnetillegg.RettTilBarnetillegg
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.BarnetilleggGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Beregningsgrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.Grunnbeløp
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.tidslinje.JoinStyle
import no.nav.aap.tidslinje.Segment
import no.nav.aap.tidslinje.Tidslinje
import no.nav.aap.verdityper.Beløp
import no.nav.aap.verdityper.GUnit
import no.nav.aap.verdityper.Periode
import no.nav.aap.verdityper.Prosent


class BeregnTilkjentYtelseService(
    private val fødselsdato: Fødselsdato,
    private val beregningsgrunnlag: Beregningsgrunnlag?,
    private val underveisgrunnlag: UnderveisGrunnlag,
    private val barnetilleggGrunnlag: BarnetilleggGrunnlag
) {

    private fun tilTidslinje(barnetilleggGrunnlag: BarnetilleggGrunnlag): Tidslinje<RettTilBarnetillegg> {
        return Tidslinje(
            barnetilleggGrunnlag.perioder.map {
                Segment(
                    it.periode,
                    RettTilBarnetillegg(
                        it.personIdenter
                    )
                )
            }
        )
    }

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
        val barnetilleggGrunnlagTidslinje = tilTidslinje(barnetilleggGrunnlag)
        val utgangspunktForÅrligYtelse = grunnlagsfaktor?.multiplisert(Prosent.`66_PROSENT`) ?: GUnit(0)

        val minsteÅrligYtelseMedAlderTidslinje = minsteÅrligYtelseAlderStrategiTidslinje.kombiner(
            MINSTE_ÅRLIG_YTELSE_TIDSLINJE,
            AldersjusteringAvMinsteÅrligYtelse
        )

        // TODO: Vurder om vi her skal ha en egen tidslinje for "ikke rett" som gir 0 i grunnnlag
        val årligYtelseTidslinje = minsteÅrligYtelseMedAlderTidslinje.mapValue { minsteÅrligYtelse ->
            maxOf(minsteÅrligYtelse, utgangspunktForÅrligYtelse)
        }

        val gradertÅrligYtelseTidslinje = underveisTidslinje.kombiner(
            årligYtelseTidslinje,
            JoinStyle.INNER_JOIN { periode, venstre, høyre ->
                val dagsats = høyre.verdi.dividert(ANTALL_ÅRLIGE_ARBEIDSDAGER)
                val utbetalingsgrad = venstre.verdi.utbetalingsgrad()
                Segment(periode, TilkjentGUnit(dagsats, utbetalingsgrad))
            })


        val gradertÅrligTilkjentYtelseBeløp = gradertÅrligYtelseTidslinje.kombiner(
            Grunnbeløp.tilTidslinje(),
            JoinStyle.INNER_JOIN { periode, venstre, høyre ->
                val dagsats = høyre.verdi.multiplisert(venstre.verdi.dagsats)

                val utbetalingsgrad = venstre.verdi.gradering
                Segment(
                    periode, TilkjentFørBarn(
                        dagsats = dagsats,
                        gradering = utbetalingsgrad,
                        grunnlag = dagsats,
                        grunnlagsfaktor = venstre.verdi.dagsats,
                        grunnbeløp = høyre.verdi
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
                val dagsats = venstre.verdi.dagsats
                val gradering = venstre.verdi.gradering
                Segment(
                    periode, Tilkjent(
                        dagsats = dagsats,
                        gradering = gradering,
                        barnetillegg = høyre?.verdi?.barnetillegg ?: Beløp(0),
                        grunnlagsfaktor = venstre.verdi.grunnlagsfaktor,
                        grunnlag = venstre.verdi.grunnlag,
                        grunnbeløp = venstre.verdi.grunnbeløp,
                        antallBarn = høyre?.verdi?.antallBarn ?: 0,
                        barnetilleggsats = høyre?.verdi?.barnetilleggsats ?: Beløp(0)
                    )
                )
            })
    }

    private class TilkjentFørBarn(
        val dagsats: Beløp,
        val gradering: Prosent,
        val grunnlag: Beløp,
        val grunnlagsfaktor: GUnit,
        val grunnbeløp: Beløp,
    )

    private class Barnetillegg(
        val antallBarn: Int,
        val barnetilleggsats: Beløp,
        val barnetillegg: Beløp
    )
}
