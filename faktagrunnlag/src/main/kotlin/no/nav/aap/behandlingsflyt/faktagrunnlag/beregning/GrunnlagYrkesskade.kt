package no.nav.aap.behandlingsflyt.faktagrunnlag.beregning

import no.nav.aap.behandlingsflyt.faktagrunnlag.vilkårsresultat.Faktagrunnlag
import no.nav.aap.verdityper.GUnit
import java.math.BigDecimal

class GrunnlagYrkesskade(
    private val grunnlaget: GUnit,
    private val beregningsgrunnlag: Grunnlag11_19
) : Beregningsgrunnlag {

    override fun grunnlaget(): GUnit {
        return grunnlaget
    }

    override fun faktagrunnlag(): Faktagrunnlag {
        return Fakta(
            grunnlaget = grunnlaget.verdi(),
            beregningsgrunnlag = beregningsgrunnlag.faktagrunnlag()
        )
    }

    internal class Fakta(
        // FIXME: BigDecimal serialiseres til JSON på standardform
        val grunnlaget: BigDecimal,
        val beregningsgrunnlag: Faktagrunnlag
    ) : Faktagrunnlag

    fun underliggende(): Grunnlag11_19 {
        return beregningsgrunnlag
    }

    override fun toString(): String {
        return "GrunnlagYrkesskade(grunnlaget=$grunnlaget, beregningsgrunnlag=$beregningsgrunnlag)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GrunnlagYrkesskade

        if (grunnlaget != other.grunnlaget) return false
        if (beregningsgrunnlag != other.beregningsgrunnlag) return false

        return true
    }

    override fun hashCode(): Int {
        var result = grunnlaget.hashCode()
        result = 31 * result + beregningsgrunnlag.hashCode()
        return result
    }
}
