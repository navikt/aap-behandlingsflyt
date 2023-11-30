package no.nav.aap.behandlingsflyt.beregning

import no.nav.aap.behandlingsflyt.faktagrunnlag.inntekt.GUnit
import no.nav.aap.behandlingsflyt.faktagrunnlag.inntekt.adapter.InntektPerÅr
import java.math.BigDecimal
import java.util.*

class GrunnlagetForBeregningen(
    inntekter: List<InntektPerÅr>
) {
    private val inntekter: SortedSet<InntektPerÅr> =
        inntekter.toSortedSet(Comparator.comparing(InntektPerÅr::år).reversed())

    init {
        require(inntekter.size == this.inntekter.size) { "Flere inntekter oppgitt for samme år" }
    }

    fun beregnGrunnlaget(): GUnit {
        if (inntekter.isEmpty()) {
            return GUnit(BigDecimal(0))
        }

        val gUnits = inntekter.map { inntekt ->
            Grunnbeløp.finnGrunnlagsfaktor(inntekt.år, inntekt.beløp)
        }

        val gUnitsBegrensetTil6GUnits = gUnits.map(GUnit::begrensTil6GUnits)

        val gUnitFørsteÅr = gUnitsBegrensetTil6GUnits.first()

        if (inntekter.size == 1) {
            return gUnitFørsteÅr
        }

        val gUnitGjennomsnitt = GUnit.gjennomsnittlig(gUnitsBegrensetTil6GUnits)

        return maxOf(gUnitFørsteÅr, gUnitGjennomsnitt)
    }
}
