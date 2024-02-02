package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning

import no.nav.aap.behandlingsflyt.faktagrunnlag.usorterte.inntekt.Grunnbeløp
import no.nav.aap.verdityper.GUnit
import java.util.*

class GrunnlagetForBeregningen(
    inntekter: Set<no.nav.aap.behandlingsflyt.faktagrunnlag.usorterte.inntekt.InntektPerÅr>
) {
    private val inntekter: SortedSet<no.nav.aap.behandlingsflyt.faktagrunnlag.usorterte.inntekt.InntektPerÅr> =
        inntekter.toSortedSet().reversed()

    init {
        require(this.inntekter.size == 3) { "Må oppgi tre inntekter" }
        require(this.inntekter.first().år == this.inntekter.last().år.plusYears(2)) { "Inntektene må representere tre sammenhengende år" }
    }

    fun beregnGrunnlaget(): Grunnlag11_19 {
        val gUnits = inntekter.map { inntekt ->
            Grunnbeløp.finnGUnit(inntekt.år, inntekt.beløp)
        }

        val gUnitsBegrensetTil6GUnits = gUnits.map(GUnit::begrensTil6GUnits)

        val gUnitFørsteÅr = gUnitsBegrensetTil6GUnits.first()

        val gUnitGjennomsnitt = GUnit.gjennomsnittlig(gUnitsBegrensetTil6GUnits)

        return Grunnlag11_19(
            maxOf(
                gUnitFørsteÅr,
                gUnitGjennomsnitt
            )
        )
    }
}
