package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Faktagrunnlag
import no.nav.aap.verdityper.GUnit
import java.math.BigDecimal
import java.time.Year

class Grunnlag11_19(
    private val grunnlaget: GUnit,
    private val er6GBegrenset: Boolean,
    private val erGjennomsnitt: Boolean
) : Beregningsgrunnlag {

    override fun grunnlaget(): GUnit {
        return grunnlaget
    }

    override fun faktagrunnlag(): Faktagrunnlag {
        return Fakta(
            grunnlaget = grunnlaget.verdi()
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
        val grunnlaget: BigDecimal
    ) : Faktagrunnlag

    override fun toString(): String {
        return "Grunnlag11_19(grunnlaget=$grunnlaget)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Grunnlag11_19

        return grunnlaget == other.grunnlaget
    }

    override fun hashCode(): Int {
        return grunnlaget.hashCode()
    }
}
