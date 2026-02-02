package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning

import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag
import no.nav.aap.komponenter.verdityper.GUnit
import java.math.BigDecimal
import kotlin.Boolean

/**
 * Grunnlag-data relatert til §11-19.
 *
 * @param grunnlaget Hvilket grunnlag som beregningen skal basere seg utfra §11-19.
 * @param erGjennomsnitt Om [grunnlaget] er et gjennomsnitt.
 * @param gjennomsnittligInntektIG Gjennomsnittlig inntekt målt i G over siste 3 år.
 * @param inntekter Inntekter de siste 3 år.
 */
data class Grunnlag11_19(
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
            grunnlaget = grunnlaget.verdi(),
            erGjennomsnitt = erGjennomsnitt,
            gjennomsnittligInntektIG = gjennomsnittligInntektIG,
            inntekter = inntekter,
        )
    }

    fun erGjennomsnitt(): Boolean {
        return erGjennomsnitt
    }

    internal class Fakta(
        val grunnlaget: BigDecimal,
        val erGjennomsnitt: Boolean,
        val gjennomsnittligInntektIG: GUnit,
        val inntekter: List<GrunnlagInntekt>,
    ) : Faktagrunnlag
}
