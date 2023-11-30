package no.nav.aap.behandlingsflyt.beregning

import no.nav.aap.behandlingsflyt.faktagrunnlag.inntekt.GUnit
import no.nav.aap.behandlingsflyt.faktagrunnlag.inntekt.adapter.InntektPerÅr
import java.util.*

class GrunnlagetForBeregningen(
    inntekter: List<InntektPerÅr>
) {
    private val inntekter: SortedSet<InntektPerÅr> =
        inntekter.toSortedSet(Comparator.comparing(InntektPerÅr::år).reversed())

    init {
        require(this.inntekter.size == 3) { "Må oppgi tre inntekter" }
        require(this.inntekter.first().år == this.inntekter.last().år.plusYears(2)) { "Inntektene må representere tre sammenhengende år" }
        require(this.inntekter.size == inntekter.size) { "Flere inntekter oppgitt for samme år" }
    }

    fun beregnGrunnlaget(): GUnit {
        val gUnits = inntekter.map { inntekt ->
            Grunnbeløp.finnGUnit(inntekt.år, inntekt.beløp)
        }

        val gUnitsBegrensetTil6GUnits = gUnits.map(GUnit::begrensTil6GUnits)

        val gUnitFørsteÅr = gUnitsBegrensetTil6GUnits.first()

        val gUnitGjennomsnitt = GUnit.gjennomsnittlig(gUnitsBegrensetTil6GUnits)

        return maxOf(gUnitFørsteÅr, gUnitGjennomsnitt)
    }
}
