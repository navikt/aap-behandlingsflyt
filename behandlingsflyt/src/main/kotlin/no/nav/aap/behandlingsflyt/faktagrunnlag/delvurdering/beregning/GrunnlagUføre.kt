package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning

import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.Uføre
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
 * @param uføregrader Uføregrad i prosent, sammen med virkningstidspunkt.
 * @param uføreInntekterFraForegåendeÅr Inntekter de siste 3 år før [uføreYtterligereNedsattArbeidsevneÅr].
 * @param uføreYtterligereNedsattArbeidsevneÅr Hvilket år arbeidsevnen ble ytterligere nedsatt.
 */
data class GrunnlagUføre(
    private val grunnlaget: GUnit,
    private val type: Type,
    private val grunnlag: Grunnlag11_19,
    private val grunnlagYtterligereNedsatt: Grunnlag11_19,
    private val uføreInntekterFraForegåendeÅr: List<UføreInntekt>,
    private val uføreYtterligereNedsattArbeidsevneÅr: Year,
    private val uføregrader: Set<Uføre>
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

    fun uføregrader(): Set<Uføre> {
        return uføregrader
    }

    fun uføreInntekterFraForegåendeÅr(): List<UføreInntekt> {
        return uføreInntekterFraForegåendeÅr
    }

    fun uføreYtterligereNedsattArbeidsevneÅr(): Year {
        return uføreYtterligereNedsattArbeidsevneÅr
    }

    internal data class Fakta(
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
