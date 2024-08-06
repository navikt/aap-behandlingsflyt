package no.nav.aap.behandlingsflyt.behandling.beregning

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Grunnlag11_19
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.Grunnbeløp
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektPerÅr
import no.nav.aap.verdityper.GUnit
import java.util.*

/**
 * Klassen er ansvarlig for §11-19-delen av beregningen.
 *
 * @param inntekter Inntekter de 3 foregående årene _før_ nedsettelsen i arbeidsevne.
 */
class GrunnlagetForBeregningen(
    inntekter: Set<InntektPerÅr>
) {
    // Inntekter er sortert etter nyeste år først.
    private val inntekter: SortedSet<InntektPerÅr> =
        inntekter.toSortedSet().reversed()

    init {
        require(this.inntekter.size == 3) { "Må oppgi tre inntekter" }
        require(this.inntekter.first().år == this.inntekter.last().år.plusYears(2)) { "Inntektene må representere tre sammenhengende år" }
    }

    /**
     * Beregn grunnlaget etter §11-19. Implementering av https://lovdata.no/lov/1997-02-28-19/§11-19
     */
    fun beregnGrunnlaget(): Grunnlag11_19 {
        // Inntekter justeres etter størrelsen på G-beløpet i de aktuelle årene.
        val gUnits = inntekter.map { inntekt ->
            Grunnbeløp.finnGUnit(inntekt.år, inntekt.beløp)
        }

        val gUnitsBegrensetTil6GUnits = gUnits.map(GUnit::begrensTil6GUnits) // todo: her bør er6gbegrenset være
        val er6Gbegrenset = gUnitsBegrensetTil6GUnits != gUnits
        val gUnitFørsteÅr = gUnitsBegrensetTil6GUnits.first()

        val gUnitGjennomsnitt = GUnit.gjennomsnittlig(gUnitsBegrensetTil6GUnits)

        // Om gjennomsnittlig inntekt siste tre år er høyere enn siste års inntekt, skal den brukes i beregningen
        // i stedet.
        var gjeldende = gUnitFørsteÅr
        var erGjennomsnitt = false
        if (gUnitFørsteÅr < gUnitGjennomsnitt) {
            erGjennomsnitt = true
            gjeldende = gUnitGjennomsnitt
        }

        return Grunnlag11_19(
            gjeldende,
            er6GBegrenset = er6Gbegrenset, // TODO endre denne
            erGjennomsnitt = erGjennomsnitt,
            inntekter = inntekter.toList()
        )
    }
}
