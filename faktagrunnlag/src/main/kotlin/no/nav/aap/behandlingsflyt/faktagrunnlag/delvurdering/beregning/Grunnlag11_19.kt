package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Faktagrunnlag
import no.nav.aap.komponenter.verdityper.GUnit
import java.math.BigDecimal

/**
 * Grunnlag-data relatert til §11-19.
 *
 * @param grunnlaget Hvilket grunnlag som beregningen skal basere seg utfra §11-19.
 * @param erGjennomsnitt Om [grunnlaget] er et gjennomsnitt.
 * @param gjennomsnittligInntektIG Gjennomsnittlig inntekt målt i G over siste 3 år.
 * @param inntekter Inntekter de siste 3 år.
 */
class Grunnlag11_19(
    private val grunnlaget: GUnit,
    private val erGjennomsnitt: Boolean,
    private val gjennomsnittligInntektIG: GUnit,
    private val inntekter: List<GrunnlagInntekt>,
) : Beregningsgrunnlag {

    fun inntekter(): List<GrunnlagInntekt> {
        return inntekter
    }

    fun gjennomsnittligInntektIG(): GUnit {
        return gjennomsnittligInntektIG
    }

    override fun grunnlaget(): GUnit {
        return grunnlaget
    }

    override fun faktagrunnlag(): Faktagrunnlag {
        return Fakta(
            grunnlaget = grunnlaget.verdi()
        )
    }

    fun erGjennomsnitt(): Boolean {
        return erGjennomsnitt
    }

    internal class Fakta(
        // FIXME: BigDecimal serialiseres til JSON på standardform
        val grunnlaget: BigDecimal
    ) : Faktagrunnlag


    override fun toString(): String {
        return "Grunnlag11_19(grunnlaget=$grunnlaget, erGjennomsnitt=$erGjennomsnitt, inntekter=$inntekter)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Grunnlag11_19

        if (grunnlaget != other.grunnlaget) return false
        if (erGjennomsnitt != other.erGjennomsnitt) return false
        if (gjennomsnittligInntektIG != other.gjennomsnittligInntektIG) return false
        if (inntekter != other.inntekter) return false

        return true
    }

    override fun hashCode(): Int {
        var result = grunnlaget.hashCode()
        result = 31 * result + erGjennomsnitt.hashCode()
        result = 31 * result + gjennomsnittligInntektIG.hashCode()
        result = 31 * result + inntekter.hashCode()
        return result
    }

}
