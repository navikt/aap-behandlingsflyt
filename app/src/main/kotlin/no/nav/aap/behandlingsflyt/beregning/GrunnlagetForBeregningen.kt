package no.nav.aap.behandlingsflyt.beregning

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Grunnlag11_19
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.Grunnbeløp
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektPerÅr
import no.nav.aap.verdityper.GUnit
import java.util.*

class GrunnlagetForBeregningen(
    inntekter: Set<InntektPerÅr>
) {
    private val inntekter: SortedSet<InntektPerÅr> =
        inntekter.toSortedSet().reversed()

    init {
        require(this.inntekter.size == 3) { "Må oppgi tre inntekter" }
        require(this.inntekter.first().år == this.inntekter.last().år.plusYears(2)) { "Inntektene må representere tre sammenhengende år" }
    }

    fun beregnGrunnlaget(): Grunnlag11_19 {
        var er6Gbegrenset = false
        var erGjennomsnitt = false
        val gUnits = inntekter.map { inntekt ->
            Grunnbeløp.finnGUnit(inntekt.år, inntekt.beløp)
        }

        val gUnitsBegrensetTil6GUnits = gUnits.map(GUnit::begrensTil6GUnits)
        er6Gbegrenset = gUnitsBegrensetTil6GUnits == gUnits
        val gUnitFørsteÅr = gUnitsBegrensetTil6GUnits.first()

        val gUnitGjennomsnitt = GUnit.gjennomsnittlig(gUnitsBegrensetTil6GUnits)
        val gjeldende = maxOf(
            gUnitFørsteÅr,
            gUnitGjennomsnitt
        )

        erGjennomsnitt = gjeldende == gUnitGjennomsnitt

        return Grunnlag11_19(
            gjeldende,
            er6GBegrenset= er6Gbegrenset,
            erGjennomsnitt = erGjennomsnitt
        )
    }
}