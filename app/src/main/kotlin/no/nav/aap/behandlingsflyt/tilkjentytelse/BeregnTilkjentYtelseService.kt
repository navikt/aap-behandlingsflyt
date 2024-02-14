package no.nav.aap.behandlingsflyt.tilkjentytelse

import no.nav.aap.behandlingsflyt.barnetillegg.RettTilBarnetillegg
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.BarnetilleggGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Beregningsgrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.Grunnbeløp
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.Tilkjent
import no.nav.aap.behandlingsflyt.forretningsflyt.steg.TilkjentGUnit
import no.nav.aap.tidslinje.JoinStyle
import no.nav.aap.tidslinje.Segment
import no.nav.aap.tidslinje.SegmentSammenslåer
import no.nav.aap.tidslinje.Tidslinje
import no.nav.aap.verdityper.Beløp
import no.nav.aap.verdityper.GUnit
import no.nav.aap.verdityper.Periode
import no.nav.aap.verdityper.Prosent


class BeregnTilkjentYtelseService(
    private val fødselsdato: Fødselsdato,
    private val beregningsgrunnlag: Beregningsgrunnlag,
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
            SegmentSammenslåer<AlderStrategi, GUnit, GUnit> {
            override fun sammenslå(
                periode: Periode,
                venstreSegment: Segment<AlderStrategi>?,
                høyreSegment: Segment<GUnit>?
            ): Segment<GUnit> {
                val minsteÅrligYtelse = requireNotNull(høyreSegment?.verdi)
                val aldersfunksjon = requireNotNull(venstreSegment?.verdi)
                return Segment(periode, aldersfunksjon.aldersjustering(minsteÅrligYtelse))
            }
        }
    }

    fun beregnTilkjentYtelse(): Tidslinje<Tilkjent> {
        val minsteÅrligYtelseAlderStrategiTidslinje = MinsteÅrligYtelseAlderTidslinje(fødselsdato).tilTidslinje()
        val underveisTidslinje = Tidslinje(underveisgrunnlag.perioder.map { Segment(it.periode, it) })
        val grunnlagsfaktor = beregningsgrunnlag.grunnlaget()
        val barnetilleggGrunnlagTidslinje = tilTidslinje(barnetilleggGrunnlag)
        val utgangspunktForÅrligYtelse = grunnlagsfaktor.multiplisert(Prosent.`66_PROSENT`)

        val minsteÅrligYtelseMedAlderTidslinje = minsteÅrligYtelseAlderStrategiTidslinje.kombiner(
            MINSTE_ÅRLIG_YTELSE_TIDSLINJE,
            AldersjusteringAvMinsteÅrligYtelse
        )

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


        val gradertÅrligTilkjentYtelseBeløp = gradertÅrligYtelseTidslinje.kombiner(
            Grunnbeløp.tilTidslinje(),
            JoinStyle.INNER_JOIN
        ) { periode, venstre, høyre ->
            val dagsats =
                høyre?.verdi?.multiplisert(requireNotNull(venstre?.verdi?.dagsats)) ?: Beløp(0)

            val utbetalingsgrad = venstre?.verdi?.gradering ?: Prosent.`0_PROSENT`
            Segment(periode, Tilkjent(dagsats, utbetalingsgrad))
        }

        val barnetilleggTidslinje = BARNETILLEGGSATS_TIDSLINJE.kombiner(
            barnetilleggGrunnlagTidslinje,
            JoinStyle.INNER_JOIN
        ){ periode, venstre, høyre ->
            Segment(periode, venstre?.verdi?.multiplisert(høyre?.verdi?.barn()?.size?:0)?:Beløp(0))
        }

        return gradertÅrligTilkjentYtelseBeløp.kombiner(
            barnetilleggTidslinje,
            JoinStyle.INNER_JOIN
        ){ periode, venstre, høyre ->
            val dagsats = venstre?.verdi?.dagsats?.pluss(høyre!!.verdi)?:Beløp(0)
            val gradering = venstre?.verdi?.gradering?:Prosent.`0_PROSENT`
            Segment(periode, Tilkjent(dagsats,gradering))
        }
    }

}