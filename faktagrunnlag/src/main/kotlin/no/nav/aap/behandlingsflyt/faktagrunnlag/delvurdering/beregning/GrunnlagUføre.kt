package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.GrunnlagUføre.Type
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Faktagrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektPerÅr
import no.nav.aap.verdityper.Beløp
import no.nav.aap.verdityper.GUnit
import no.nav.aap.verdityper.Prosent
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
 * @param uføreInntektIKroner I dag er dette det oppjusterte grunnlaget multiplisert med 10. Så gir lite mening. TODO!
 * @param uføreYtterligereNedsattArbeidsevneÅr Hvilket år arbeidsevnen ble ytterligere nedsatt.
 * @param er6GBegrenset Om grunnlaget fra [grunnlaget] er 6G-begrenset. // TODO: denne er overflødig, ligger i [grunnlaget]
 * @param erGjennomsnitt Om grunnlaget fra [grunnlaget] er et gjennomsnitt. // TODO: også overflødig
 */
class GrunnlagUføre(
    private val grunnlaget: GUnit,
    private val type: Type,
    private val grunnlag: Grunnlag11_19,
    private val grunnlagYtterligereNedsatt: Grunnlag11_19,
    private val uføregrad: Prosent,
    private val uføreInntekterFraForegåendeÅr: List<InntektPerÅr>, // uføre ikke oppjustert
    private val uføreInntektIKroner: Beløp, // grunnlaget
    private val uføreYtterligereNedsattArbeidsevneÅr: Year,
    private val erGjennomsnitt: Boolean,
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

    fun uføreInntektIKroner(): Beløp {
        return uføreInntektIKroner
    }

    fun uføregrad(): Prosent {
        return uføregrad
    }

    fun uføreYtterligereNedsattArbeidsevneÅr(): Year {
        return uføreYtterligereNedsattArbeidsevneÅr
    }

    override fun erGjennomsnitt(): Boolean {
        return erGjennomsnitt
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

    override fun toString(): String {
        return "GrunnlagUføre(grunnlaget=$grunnlaget, gjeldende=$type, grunnlag=$grunnlag, grunnlagYtterligereNedsatt=$grunnlagYtterligereNedsatt)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GrunnlagUføre

        if (grunnlaget != other.grunnlaget) return false
        if (type != other.type) return false
        if (grunnlag != other.grunnlag) return false
        if (grunnlagYtterligereNedsatt != other.grunnlagYtterligereNedsatt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = grunnlaget.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + grunnlag.hashCode()
        result = 31 * result + grunnlagYtterligereNedsatt.hashCode()
        return result
    }
}
