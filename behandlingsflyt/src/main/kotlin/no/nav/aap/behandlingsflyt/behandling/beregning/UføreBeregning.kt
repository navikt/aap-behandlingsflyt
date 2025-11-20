package no.nav.aap.behandlingsflyt.behandling.beregning

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Grunnlag11_19
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.GrunnlagUføre
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.UføreInntekt
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.UføreInntektPeriodisert
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.Grunnbeløp
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektPerÅr
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.Uføre
import no.nav.aap.komponenter.tidslinje.JoinStyle
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.komponenter.verdityper.Tid
import java.time.Year

class UføreBeregning(
    private val grunnlag: Grunnlag11_19,
    private val uføregrader: Set<Uføre>,
    private val relevanteÅr: Set<Year>,
    private val inntektsPerioder: Set<InntektsPeriode>,
) {

    fun beregnUføre(ytterligereNedsattÅr: Year): GrunnlagUføre {
        // tidslinjelogikken er basert på antagelsen om at uføre alltid har virkningstidspunkt på den første i måneden
        // og at vi får inntekt fra inntektskomponenten per måned
        val inntektTidslinje = inntektTidslinje(inntektsPerioder)
        val uføreTidslinje = lagUføreTidslinje(uføregrader)
        val oppjusterteInntekter =
            oppjusterMhpUføregradPeriodisertInntekt(
                relevanteÅr,
                inntektTidslinje,
                uføreTidslinje
            )

        // 6G-begrensning ligger her samt gjennomsnitt
        val ytterligereNedsattGrunnlag = beregn11_19Grunnlag(oppjusterteInntekter)

        val (grunnlaget, type) =
            if (grunnlag.grunnlaget() >= ytterligereNedsattGrunnlag.grunnlaget()) {
                Pair(grunnlag.grunnlaget(), GrunnlagUføre.Type.STANDARD)
            } else Pair(ytterligereNedsattGrunnlag.grunnlaget(), GrunnlagUføre.Type.YTTERLIGERE_NEDSATT)

        return GrunnlagUføre(
            grunnlaget = grunnlaget,
            type = type,
            grunnlag = grunnlag,
            grunnlagYtterligereNedsatt = ytterligereNedsattGrunnlag,
            uføregrad = uføregrader.maxBy { it.virkningstidspunkt }.uføregrad,
            uføreInntekterFraForegåendeÅr = oppjusterteInntekter,
            uføreYtterligereNedsattArbeidsevneÅr = ytterligereNedsattÅr
        )
    }

    private fun oppjusterMhpUføregradPeriodisertInntekt(
        ikkeOppjusterteInntekter: Set<Year>,
        inntektTidslinje: Tidslinje<InntektData>,
        uføreTidslinje: Tidslinje<Prosent>
    ): List<UføreInntekt> {
        val oppjusterteInntekterTidslinje =
            inntektTidslinje.kombiner(uføreTidslinje, JoinStyle.LEFT_JOIN { periode, venstre, høyre ->
                val inntektIPeriode = Beløp(venstre.verdi.beløp.toBigDecimal())

                val uføregrad = høyre?.verdi ?: Prosent.`0_PROSENT`
                val arbeidsgrad = uføregrad.komplement()

                Segment(
                    periode, UføreInntektPeriodisert(
                        periode = periode,
                        inntektIKroner = inntektIPeriode,
                        uføregrad = uføregrad,
                        inntektJustertForUføregrad = if (arbeidsgrad.prosentverdi() == 0) {
                            Beløp(0)
                        } else {
                            inntektIPeriode.dividert(arbeidsgrad)
                        }
                    )
                )
            })

        return ikkeOppjusterteInntekter.map {
            oppjusterteInntekterTidslinje.begrensetTil(Periode(it.atDay(1), it.plusYears(1).atDay(1).minusDays(1)))
        }.map {
            val summertInntektJustertForUføre = it.segmenter().sumOf { it.verdi.inntektJustertForUføregrad.verdi }
            val summertInntekt = it.segmenter().sumOf { it.verdi.inntektIKroner.verdi }
            val år = Year.of(it.helePerioden().fom.year)

            val summertInntektIGJustertForUføregrad = gUnit(år, Beløp(summertInntektJustertForUføre))

            UføreInntekt(
                år = år,
                inntektsPerioder = it.segmenter().map { it.verdi },
                inntektIKroner = Beløp(summertInntekt),
                inntektJustertForUføregrad = Beløp(summertInntektJustertForUføre),
                inntektIGJustertForUføregrad = summertInntektIGJustertForUføregrad.gUnit,
                inntektIG = gUnit(år, Beløp(summertInntekt)).gUnit,
                grunnbeløp = summertInntektIGJustertForUføregrad.beløp,
                uføregrad = it.segmenter().map { it.verdi.uføregrad }.lastOrNull() ?: Prosent.`0_PROSENT`
            )
        }
    }

    private fun beregn11_19Grunnlag(
        oppjusterteInntekter: List<UføreInntekt>
    ): Grunnlag11_19 {
        val oppjusterteInntekterPerÅr = oppjusterteInntekter.map { inntekt ->
            InntektPerÅr(
                år = inntekt.år,
                beløp = inntekt.inntektJustertForUføregrad
            )
        }.toSet()
        return GrunnlagetForBeregningen(oppjusterteInntekterPerÅr).beregnGrunnlaget()
    }

    private fun inntektTidslinje(inntektsPerioder: Set<InntektsPeriode>): Tidslinje<InntektData> {
        val inntektstidslinje = Tidslinje(inntektsPerioder.map {
            Segment(it.periode, InntektData(it.beløp))
        })
        return inntektstidslinje
    }

    private fun lagUføreTidslinje(uføregrader: Set<Uføre>): Tidslinje<Prosent> {
        return uføregrader.sortedBy { it.virkningstidspunkt }
            .map { vurdering ->
                Tidslinje(
                    Periode(
                        fom = vurdering.virkningstidspunkt,
                        tom = Tid.MAKS
                    ), vurdering.uføregrad
                )
            }.fold(Tidslinje()) { acc, curr ->
                acc.kombiner(curr, StandardSammenslåere.prioriterHøyreSideCrossJoin())
            }
    }

    private fun gUnit(år: Year, beløp: Beløp): Grunnbeløp.BenyttetGjennomsnittsbeløp =
        Grunnbeløp.finnGUnit(år, beløp)

}
