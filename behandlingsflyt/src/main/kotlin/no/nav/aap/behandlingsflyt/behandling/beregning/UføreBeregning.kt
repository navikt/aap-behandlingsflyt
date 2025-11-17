package no.nav.aap.behandlingsflyt.behandling.beregning

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Grunnlag11_19
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.GrunnlagUføre
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.UføreInntekt
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.UføreInntektPeriodisert
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.Grunnbeløp
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektPerÅr
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.Uføre
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
    private val inntektsPerioder: List<InntektsPeriode>,
) {

    init {
        require(uføregrader.maxBy { it.virkningstidspunkt }.uføregrad < Prosent.`100_PROSENT`)
        { "Uføregraden må være mindre enn 100 prosent" }
    }

    fun beregnUføre(ytterligereNedsattÅr: Year): GrunnlagUføre {
        // tidslinjelogikken er basert på antagelsen om at uføre alltid har virkningstidspunkt på den første i måneden
        // og at vi får inntekt fra inntektskomponenten per måned
        val tidslinjeInntektOgUføre = uføreOgInntektTidslinje(inntektsPerioder, uføregrader)
        val inntektTidslinje = inntektTidslinje(inntektsPerioder)
        val uføreTidslinje = lagUføreTidslinje(uføregrader)
        val oppjusterteInntekter =
            oppjusterMhpUføregradPeriodisertInntekt(
                relevanteÅr,
                tidslinjeInntektOgUføre,
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
        tidslinjeInntektOgUføre: Tidslinje<Pair<InntektData?, Prosent?>>,
        inntektTidslinje: Tidslinje<InntektData>,
        uføreTidslinje: Tidslinje<Prosent>
    ): List<UføreInntekt> {
        val tidslinjerPerRelevantÅr = ikkeOppjusterteInntekter
            .map { tidslinjeInntektOgUføre.begrensetTil(Periode(it.atDay(1), it.atDay(it.length()))) }

        val oppjustertePerioderPerÅr = tidslinjerPerRelevantÅr.map { årsTidslinje ->
            årsTidslinje.segmenter().map { segment ->
                val inntektIPeriode = Beløp(segment.verdi.first?.beløp?.toInt() ?: 0)
                val arbeidsgrad = segment.verdi.second?.komplement() ?: Prosent.`100_PROSENT`
                UføreInntektPeriodisert(
                    periode = segment.periode,
                    inntektIKroner = inntektIPeriode,
                    uføregrad = segment.verdi.second ?: Prosent.`0_PROSENT`,
                    inntektJustertForUføregrad = if (arbeidsgrad.prosentverdi() == 0) {
                        Beløp(0) // TODO er det riktig
                    } else {
                        inntektIPeriode.dividert(arbeidsgrad)
                    }
                )
            }
        }

        return oppjustertePerioderPerÅr.map {
            val summertInntektJustertForUføre = it.sumOf { it.inntektJustertForUføregrad.verdi }
            val summertInntekt = it.sumOf { uføreInntektPeriodisert -> uføreInntektPeriodisert.inntektIKroner.verdi }

            require(it.all { periode -> periode.periode.fom.year == it.first().periode.fom.year })

            // require(it.size == 12) ??
            val år = Year.from(it.first().periode.fom)
            val summertInntektIGJustertForUføregrad = gUnit(år, Beløp(summertInntektJustertForUføre))
            UføreInntekt(
                år = år,
                inntektsPerioder = it,
                inntektIKroner = Beløp(summertInntekt),
                inntektJustertForUføregrad = Beløp(summertInntektJustertForUføre),
                inntektIGJustertForUføregrad = summertInntektIGJustertForUføregrad.gUnit,
                inntektIG = gUnit(år, Beløp(summertInntekt)).gUnit,
                grunnbeløp = summertInntektIGJustertForUføregrad.beløp,
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

    private fun uføreOgInntektTidslinje(
        inntektsPerioder: List<InntektsPeriode>,
        uføregrader: Set<Uføre>
    ): Tidslinje<Pair<InntektData?, Prosent?>> {
        val inntektstidslinje = inntektTidslinje(inntektsPerioder)

        val uføretidslinje = lagUføreTidslinje(uføregrader)
        return Tidslinje.zip2(inntektstidslinje, uføretidslinje)
    }

    private fun inntektTidslinje(inntektsPerioder: List<InntektsPeriode>): Tidslinje<InntektData> {
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
