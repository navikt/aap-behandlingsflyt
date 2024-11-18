package no.nav.aap.behandlingsflyt.behandling.beregning

import no.nav.aap.behandlingsflyt.behandling.beregning.GrunnlagetForBeregningen.GrunnlagInntektForBeregning.Companion.beregnGjennomsnitt
import no.nav.aap.behandlingsflyt.behandling.beregning.GrunnlagetForBeregningen.GrunnlagInntektForBeregning.Companion.tilGrunnlagInntekt
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Grunnlag11_19
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.GrunnlagInntekt
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektPerÅr
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.GUnit
import java.time.Year
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
     * Beregn grunnlaget etter [§11-19](https://lovdata.no/lov/1997-02-28-19/§11-19).
     */
    fun beregnGrunnlaget(): Grunnlag11_19 {
        val beregnetInntekter = beregnInntekter()

        val inntektFørsteÅr = beregnetInntekter.first()

        val gUnitGjennomsnitt = beregnetInntekter.beregnGjennomsnitt()

        // Om gjennomsnittlig inntekt siste tre år er høyere enn siste års inntekt, skal den brukes i beregningen
        // i stedet.
        return if (inntektFørsteÅr.skalBrukeGjennomsnitt(gUnitGjennomsnitt)) {
            Grunnlag11_19(
                grunnlaget = gUnitGjennomsnitt,
                erGjennomsnitt = true,
                gjennomsnittligInntektIG = gUnitGjennomsnitt,
                inntekter = beregnetInntekter.tilGrunnlagInntekt()
            )
        } else {
            Grunnlag11_19(
                grunnlaget = inntektFørsteÅr.grunnlag(),
                erGjennomsnitt = false,
                gjennomsnittligInntektIG = gUnitGjennomsnitt,
                inntekter = beregnetInntekter.tilGrunnlagInntekt()
            )
        }
    }

    private class GrunnlagInntektForBeregning(
        private val år: Year,
        private val inntektIKroner: Beløp,
        private val grunnbeløp: Beløp,
        private val inntektIG: GUnit,
        private val inntekt6GBegrenset: GUnit,
        private val er6GBegrenset: Boolean
    ) {
        companion object {
            fun Iterable<GrunnlagInntektForBeregning>.beregnGjennomsnitt(): GUnit {
                return GUnit.gjennomsnittlig(this.map(GrunnlagInntektForBeregning::inntekt6GBegrenset))
            }

            fun Iterable<GrunnlagInntektForBeregning>.tilGrunnlagInntekt(): List<GrunnlagInntekt> {
                return map { inntekt ->
                    GrunnlagInntekt(
                        år = inntekt.år,
                        inntektIKroner = inntekt.inntektIKroner,
                        grunnbeløp = inntekt.grunnbeløp,
                        inntektIG = inntekt.inntektIG,
                        inntekt6GBegrenset = inntekt.inntekt6GBegrenset,
                        er6GBegrenset = inntekt.er6GBegrenset
                    )
                }
            }
        }

        fun skalBrukeGjennomsnitt(gjennomsnitt: GUnit): Boolean {
            return this.inntekt6GBegrenset < gjennomsnitt
        }

        fun grunnlag(): GUnit {
            return inntekt6GBegrenset
        }
    }

    private fun beregnInntekter(): List<GrunnlagInntektForBeregning> {
        return inntekter.map(::beregnInntekt)
    }

    private fun beregnInntekt(inntektPerÅr: InntektPerÅr): GrunnlagInntektForBeregning {
        val år = inntektPerÅr.år
        val inntektIKroner = inntektPerÅr.beløp
        // Inntekter justeres etter størrelsen på G-beløpet i de aktuelle årene.
        val benyttetGjennomsnittsbeløp = inntektPerÅr.gUnit()
        val inntektIG = benyttetGjennomsnittsbeløp.gUnit
        val grunnbeløp = benyttetGjennomsnittsbeløp.beløp
        val inntekt6GBegrenset = inntektIG.begrensTil6GUnits()
        val er6GBegrenset = inntektIG > inntekt6GBegrenset
        return GrunnlagInntektForBeregning(
            år = år,
            inntektIKroner = inntektIKroner,
            grunnbeløp = grunnbeløp,
            inntektIG = inntektIG,
            inntekt6GBegrenset = inntekt6GBegrenset,
            er6GBegrenset = er6GBegrenset,
        )
    }
}
