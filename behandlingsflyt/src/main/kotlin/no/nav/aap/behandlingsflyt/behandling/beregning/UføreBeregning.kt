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
import no.nav.aap.komponenter.verdityper.GUnit
import no.nav.aap.komponenter.verdityper.Prosent
import java.time.LocalDate
import java.time.Year

class UføreBeregning(
    private val grunnlag: Grunnlag11_19,
    private val uføregrader: Set<Uføre>,
    private val inntekterForegåendeÅr: Set<InntektPerÅr>,
    private val inntektsPerioder: List<InntektsPeriode>,
) {

    init {
        require(uføregrader.maxBy { it.virkningstidspunkt }.uføregrad < Prosent.`100_PROSENT`) { "Uføregraden må være mindre enn 100 prosent" }
    }

    fun beregnUføre(ytterligereNedsattÅr: Year): GrunnlagUføre {
        // tidslinjelogikken er basert på antagelsen om at uføre alltid har virkningstidspunkt på den første i måneden
        // og at vi får inntekt fra inntektskomponenten per måned
        val tidslinjeInntektOgUføre = uføreOgInntektTidslinje(inntektsPerioder, uføregrader)
        val oppjusterteInntekter = oppjusterMhpUføregradPeriodisertInntekt(inntekterForegåendeÅr, tidslinjeInntektOgUføre)

        // 6G-begrensning ligger her samt gjennomsnitt
        val ytterligereNedsattGrunnlag = beregn11_19Grunnlag(oppjusterteInntekter)

        if (grunnlag.grunnlaget() >= ytterligereNedsattGrunnlag.grunnlaget()) {
            return GrunnlagUføre(
                grunnlaget = grunnlag.grunnlaget(),
                type = GrunnlagUføre.Type.STANDARD,
                grunnlag = grunnlag,
                grunnlagYtterligereNedsatt = ytterligereNedsattGrunnlag,
                uføregrad = uføregrader.maxBy { it.virkningstidspunkt }.uføregrad,
                uføreInntekterFraForegåendeÅr = oppjusterteInntekter,
                uføreYtterligereNedsattArbeidsevneÅr = ytterligereNedsattÅr
            )
        } else {
            return GrunnlagUføre(
                grunnlaget = ytterligereNedsattGrunnlag.grunnlaget(),
                type = GrunnlagUføre.Type.YTTERLIGERE_NEDSATT,
                grunnlag = grunnlag,
                grunnlagYtterligereNedsatt = ytterligereNedsattGrunnlag,
                uføregrad = uføregrader.maxBy { it.virkningstidspunkt }.uføregrad,
                uføreInntekterFraForegåendeÅr = oppjusterteInntekter,
                uføreYtterligereNedsattArbeidsevneÅr = ytterligereNedsattÅr
            )
        }
    }

    private fun oppjusterMhpUføregradPeriodisertInntekt(ikkeOppjusterteInntekter: Set<InntektPerÅr>, tidslinjeInntektOgUføre: Tidslinje<Pair<InntektData?, Prosent?>>): List<UføreInntekt> {
        val tidslinjerPerRelevantÅr = ikkeOppjusterteInntekter.map { it.år }.map { tidslinjeInntektOgUføre.begrensetTil(Periode(it.atDay(1), it.atDay(it.length()))) }

        val oppjustertePerioderPerÅr = tidslinjerPerRelevantÅr.map { årsTidslinje ->
            årsTidslinje.segmenter().map { segment ->
                val inntektIPeriode = Beløp(segment.verdi.first?.beløp?.toInt() ?: 0)
                val arbeidsgrad = segment.verdi.second?.komplement() ?: Prosent.`100_PROSENT`
                val gUnitForPeriode = Grunnbeløp.finnGUnit(Year.from(segment.periode.fom), inntektIPeriode)
                UføreInntektPeriodisert(
                    periode = segment.periode,
                    inntektIKroner = inntektIPeriode,
                    inntektIG = gUnit(Year.from(segment.periode.fom), inntektIPeriode).gUnit, // TODO: kan g-justering ha skjedd i perioden`
                    uføregrad = segment.verdi.second ?: Prosent.`0_PROSENT`,
                    arbeidsgrad = arbeidsgrad,
                    inntektJustertForUføregrad = if (arbeidsgrad.prosentverdi() == 0) {
                        Beløp(0) // TODO er det riktig
                    } else {
                        inntektIPeriode.dividert(arbeidsgrad)
                    },
                    inntektIGJustertForUføregrad = if (arbeidsgrad.prosentverdi() == 0) {
                        GUnit(0)
                    } else {
                        gUnitForPeriode.gUnit.dividert(arbeidsgrad)
                    },
                    grunnbeløp = gUnitForPeriode.beløp
                )
            }
        }

        return oppjustertePerioderPerÅr.map {
            val summertInntektJustertForUføre = it.sumOf { it.inntektJustertForUføregrad.verdi }
            val summertInntekt = it.sumOf { it.inntektIKroner.verdi }
            val summertInntektIGJustertForUføregrad = GUnit(it.sumOf { it.inntektIGJustertForUføregrad.verdi() })
            UføreInntekt(
                år = Year.from(it.first().periode.fom),
                inntektsPerioder = it,
                inntektIKroner = Beløp(summertInntekt),
                inntektJustertForUføregrad = Beløp(summertInntektJustertForUføre),
                inntektIGJustertForUføregrad = summertInntektIGJustertForUføregrad,
                inntektIG = gUnit(Year.from(it.first().periode.fom), Beløp(summertInntekt)).gUnit,
                grunnbeløp = it.last().grunnbeløp,
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

    private fun uføreOgInntektTidslinje(inntektsPerioder: List<InntektsPeriode>, uføregrader: Set<Uføre>): Tidslinje<Pair<InntektData?, Prosent?>> {
        val inntektstidslinje = Tidslinje(inntektsPerioder.map { Segment(it.periode, InntektData(
            it.beløp,
        )) })

        val uføretidslinje = lagUføreTidslinje(uføregrader)
        return Tidslinje.zip2(inntektstidslinje, uføretidslinje)
    }

    private fun lagUføreTidslinje(uføregrader: Set<Uføre>): Tidslinje<Prosent> {
        return uføregrader.sortedBy { it.virkningstidspunkt }
            .map { vurdering ->
                Tidslinje(
                    Periode(
                        fom = vurdering.virkningstidspunkt,
                        tom = LocalDate.now().plusYears(1) // hva er en god "sluttdato"?
                    ), vurdering.uføregrad
                )
            }.fold(Tidslinje()) { acc, curr ->
                acc.kombiner(curr, StandardSammenslåere.prioriterHøyreSideCrossJoin())
            }
    }

    private fun gUnit(år: Year, beløp: Beløp): Grunnbeløp.BenyttetGjennomsnittsbeløp =
        Grunnbeløp.finnGUnit(år, beløp)

}
