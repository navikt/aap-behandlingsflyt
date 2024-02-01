package no.nav.aap.behandlingsflyt.faktagrunnlag.beregning

import no.nav.aap.behandlingsflyt.faktagrunnlag.vilkårsresultat.Faktagrunnlag
import no.nav.aap.verdityper.GUnit
import java.math.BigDecimal

class GrunnlagUføre(
    private val grunnlaget: GUnit,
    private val gjeldende: Type,
    private val grunnlag: Beregningsgrunnlag,
    private val grunnlagYtterligereNedsatt: Beregningsgrunnlag
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
            gjeldende = gjeldende,
            grunnlag = grunnlag.faktagrunnlag(),
            grunnlagYtterligereNedsatt = grunnlagYtterligereNedsatt.faktagrunnlag()
        )
    }

    internal class Fakta(
        // FIXME: BigDecimal serialiseres til JSON på standardform
        val grunnlaget: BigDecimal,
        val gjeldende: Type,
        val grunnlag: Faktagrunnlag,
        val grunnlagYtterligereNedsatt: Faktagrunnlag
    ) : Faktagrunnlag

    fun gjeldende(): Type {
        return gjeldende
    }

    fun underliggende(): Beregningsgrunnlag {
        return grunnlag
    }

    fun underliggendeYtterligereNedsatt(): Beregningsgrunnlag {
        return grunnlagYtterligereNedsatt
    }

    override fun toString(): String {
        return "GrunnlagUføre(grunnlaget=$grunnlaget, gjeldende=$gjeldende, grunnlag=$grunnlag, grunnlagYtterligereNedsatt=$grunnlagYtterligereNedsatt)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GrunnlagUføre

        if (grunnlaget != other.grunnlaget) return false
        if (gjeldende != other.gjeldende) return false
        if (grunnlag != other.grunnlag) return false
        if (grunnlagYtterligereNedsatt != other.grunnlagYtterligereNedsatt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = grunnlaget.hashCode()
        result = 31 * result + gjeldende.hashCode()
        result = 31 * result + grunnlag.hashCode()
        result = 31 * result + grunnlagYtterligereNedsatt.hashCode()
        return result
    }
}
