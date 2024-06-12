package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Faktagrunnlag
import no.nav.aap.verdityper.Beløp
import no.nav.aap.verdityper.GUnit
import no.nav.aap.verdityper.Prosent
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Year

class GrunnlagYrkesskade(
    private val grunnlaget: GUnit,
    private val beregningsgrunnlag: Beregningsgrunnlag,
    private val terskelverdiForYrkesskade: Prosent,
    private val andelSomSkyldesYrkesskade: GUnit,
    private val andelYrkesskade: Prosent,
    private val benyttetAndelForYrkesskade: Prosent,
    private val andelSomIkkeSkyldesYrkesskade: GUnit,
    private val antattÅrligInntektYrkesskadeTidspunktet: Beløp,
    private val yrkesskadeTidspunkt: Year,
    private val grunnlagForBeregningAvYrkesskadeandel: GUnit,
    private val yrkesskadeinntektIG: GUnit,
    private val grunnlagEtterYrkesskadeFordel: GUnit,
    private val er6GBegrenset: Boolean,
    private val erGjennomsnitt: Boolean
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

    override fun er6GBegrenset(): Boolean {
        return er6GBegrenset
    }

    override fun erGjennomsnitt(): Boolean {
        return erGjennomsnitt
    }

    internal class Fakta(
        // FIXME: BigDecimal serialiseres til JSON på standardform
        val grunnlaget: BigDecimal,
        val beregningsgrunnlag: Faktagrunnlag
    ) : Faktagrunnlag

    fun underliggende(): Beregningsgrunnlag {
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
