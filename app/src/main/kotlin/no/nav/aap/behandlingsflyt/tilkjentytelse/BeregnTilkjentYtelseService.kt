package no.nav.aap.behandlingsflyt.tilkjentytelse

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Beregningsgrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.år.MinsteÅrligYtelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.Grunnbeløp
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.Tilkjent
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.TilkjentGUnit
import no.nav.aap.tidslinje.JoinStyle
import no.nav.aap.tidslinje.Segment
import no.nav.aap.tidslinje.Tidslinje
import no.nav.aap.verdityper.Beløp
import no.nav.aap.verdityper.GUnit
import no.nav.aap.verdityper.Prosent



class BeregnTilkjentYtelseService(
    private val fødselsdato: Fødselsdato,
    private val beregningsgrunnlag: Beregningsgrunnlag,
    private val underveisgrunnlag: UnderveisGrunnlag
){
    private companion object {
        private const val ANTALL_ÅRLIGE_ARBEIDSDAGER = 260
    }

    fun beregnTilkjentYtelse(): Tidslinje<Tilkjent>{
        val minsteÅrligYtelseAlderStrategiTidslinje = MinsteÅrligYtelseAlderTidslinje(fødselsdato).tilTidslinje()
        val underveisTidslinje = Tidslinje(underveisgrunnlag.perioder.map { Segment(it.periode, it) })
        val grunnlagsfaktor = beregningsgrunnlag.grunnlaget()

        val utgangspunktForÅrligYtelse = grunnlagsfaktor.multiplisert(Prosent.`66_PROSENT`)
        val minsteÅrligYtelseTidslinje = MinsteÅrligYtelse.tilTidslinje()

        val minsteÅrligYtelseMedAlderTidslinje = minsteÅrligYtelseAlderStrategiTidslinje.kombiner(
            minsteÅrligYtelseTidslinje,
            JoinStyle.INNER_JOIN
        ) { periode, venstre:Segment<MinsteÅrligYtelseStrategi>?, høyre: Segment<GUnit>? ->
            val minsteÅrligYtelse = requireNotNull(høyre?.verdi)
            val aldersfunksjon = requireNotNull(venstre?.verdi)
            Segment(periode, aldersfunksjon.aldersjustertMinsteÅrligYtelse(minsteÅrligYtelse))
        }

        val årligYtelseTidslinje = minsteÅrligYtelseMedAlderTidslinje.mapValue { minsteÅrligYtelse ->
            maxOf(requireNotNull(minsteÅrligYtelse), utgangspunktForÅrligYtelse)
        }

        val gradertÅrligYtelseTidslinje = underveisTidslinje.kombiner(
            årligYtelseTidslinje,
            JoinStyle.INNER_JOIN
        ) { periode, venstre, høyre ->
            val dagsats = høyre?.verdi?.dividert(ANTALL_ÅRLIGE_ARBEIDSDAGER) ?: GUnit(0)
            val utbetalingsgrad = venstre?.verdi?.utbetalingsgrad() ?: Prosent.`0_PROSENT`
            Segment(periode, TilkjentGUnit(dagsats, utbetalingsgrad))
        }


        return gradertÅrligYtelseTidslinje.kombiner(
                Grunnbeløp.tilTidslinje(),
                JoinStyle.INNER_JOIN
            ) { periode, venstre, høyre ->
                val dagsats =
                    høyre?.verdi?.multiplisert(requireNotNull(venstre?.verdi?.dagsats)) ?: Beløp(0)

                val utbetalingsgrad = venstre?.verdi?.gradering ?: Prosent.`0_PROSENT`
                Segment(periode, Tilkjent(dagsats, utbetalingsgrad))
            }
    }
}