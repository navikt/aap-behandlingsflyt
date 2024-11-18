package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning

import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.GUnit
import java.time.Year

/**
 * @param år Hvilket år denne inntekten gjelder for.
 * @param inntektIKroner Inntekt i kroner.
 * @param grunnbeløp Grunnbeløp i det gjeldende året.
 * @param inntektIG Inntekt i G _for det gjeldende året_.
 * @param inntekt6GBegrenset Inntekten, oppad begrenset til 6G.
 * @param er6GBegrenset Om inntekten ble 6G-begrenset.
 */
class GrunnlagInntekt(
    val år: Year,
    val inntektIKroner: Beløp,
    val grunnbeløp: Beløp,
    val inntektIG: GUnit,
    val inntekt6GBegrenset: GUnit,
    val er6GBegrenset: Boolean
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GrunnlagInntekt

        if (år != other.år) return false
        if (inntektIKroner != other.inntektIKroner) return false
        if (grunnbeløp != other.grunnbeløp) return false
        if (inntektIG != other.inntektIG) return false
        if (inntekt6GBegrenset != other.inntekt6GBegrenset) return false
        if (er6GBegrenset != other.er6GBegrenset) return false

        return true
    }

    override fun hashCode(): Int {
        var result = år.hashCode()
        result = 31 * result + inntektIKroner.hashCode()
        result = 31 * result + inntektIG.hashCode()
        result = 31 * result + grunnbeløp.hashCode()
        result = 31 * result + inntekt6GBegrenset.hashCode()
        result = 31 * result + er6GBegrenset.hashCode()
        return result
    }

    override fun toString(): String {
        return "GrunnlagInntekt(år=$år, inntektIKroner=$inntektIKroner, grunnbeløp=$grunnbeløp, inntektIG=$inntektIG, inntekt6GBegrenset=$inntekt6GBegrenset, er6GBegrenset=$er6GBegrenset)"
    }
}
