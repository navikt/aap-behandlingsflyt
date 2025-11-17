package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning

import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag
import no.nav.aap.komponenter.verdityper.GUnit
import no.nav.aap.komponenter.verdityper.Prosent
import java.math.BigDecimal
import java.time.Year

/**
 * @param grunnlaget Beregningsgrunnlag, muligens oppjustert etter uføregrad.
 * @param type Om uføregrad er oppjustert eller ikke. [Type.STANDARD] betyr at justering etter uføregrad ikke
 * ga høyere grunnlag.
 * @param grunnlag Originalt beregningsgrunnlag, etter §11-19.
 * @param grunnlagYtterligereNedsatt Beregningsgrunnlag, oppjustert etter uføregrad.
 * @param uføregrad Uføregrad i prosent.
 * @param uføreInntekterFraForegåendeÅr Inntekter de siste 3 år før [uføreYtterligereNedsattArbeidsevneÅr].
 * @param uføreYtterligereNedsattArbeidsevneÅr Hvilket år arbeidsevnen ble ytterligere nedsatt.
 */
data class GrunnlagUføre(
    private val grunnlaget: GUnit,
    private val type: Type,
    private val grunnlag: Grunnlag11_19,
    private val grunnlagYtterligereNedsatt: Grunnlag11_19,
    private val uføregrad: Prosent,
    private val uføreInntekterFraForegåendeÅr: List<UføreInntekt>, // uføre før og etter oppjustering
    private val uføreYtterligereNedsattArbeidsevneÅr: Year
) : Beregningsgrunnlag {

    enum class Type {
        STANDARD, YTTERLIGERE_NEDSATT
    }

    override fun grunnlaget(): GUnit {
        return grunnlaget
    }

    override fun faktagrunnlag(): Faktagrunnlag {
        return Fakta(
            grunnlaget = grunnlaget.verdi(),
            grunnlag = grunnlag.faktagrunnlag(),
            grunnlagYtterligereNedsatt = grunnlagYtterligereNedsatt.faktagrunnlag()
        )
    }

    // TODO, denne burde fjernes
    @Deprecated("Bruk periodisert")
    fun uføregrad(): Prosent {
        return uføregrad
    }

    fun uføreInntekterFraForegåendeÅr(): List<UføreInntekt> {
        return uføreInntekterFraForegåendeÅr.toList()
    }

    fun uføreYtterligereNedsattArbeidsevneÅr(): Year {
        return uføreYtterligereNedsattArbeidsevneÅr
    }

    internal class Fakta(
        // FIXME: BigDecimal serialiseres til JSON på standardform
        val grunnlaget: BigDecimal,
        val grunnlag: Faktagrunnlag,
        val grunnlagYtterligereNedsatt: Faktagrunnlag
    ) : Faktagrunnlag

    fun type(): Type {
        return type
    }

    fun underliggende(): Grunnlag11_19 {
        return grunnlag
    }

    fun underliggendeYtterligereNedsatt(): Grunnlag11_19 {
        return grunnlagYtterligereNedsatt
    }
}
