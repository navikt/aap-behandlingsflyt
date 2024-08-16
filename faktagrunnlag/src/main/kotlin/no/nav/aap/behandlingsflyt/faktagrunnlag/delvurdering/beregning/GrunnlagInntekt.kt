package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning

import no.nav.aap.verdityper.Beløp
import no.nav.aap.verdityper.GUnit
import java.time.Year

class GrunnlagInntekt(
    val år: Year,
    val inntektIKroner: Beløp,
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
        if (inntektIG != other.inntektIG) return false
        if (inntekt6GBegrenset != other.inntekt6GBegrenset) return false
        if (er6GBegrenset != other.er6GBegrenset) return false

        return true
    }

    override fun hashCode(): Int {
        var result = år.hashCode()
        result = 31 * result + inntektIKroner.hashCode()
        result = 31 * result + inntektIG.hashCode()
        result = 31 * result + inntekt6GBegrenset.hashCode()
        result = 31 * result + er6GBegrenset.hashCode()
        return result
    }

    override fun toString(): String {
        return "GrunnlagInntekt(år=$år, inntektIKroner=$inntektIKroner, inntektIG=$inntektIG, inntekt6GBegrenset=$inntekt6GBegrenset, er6GBegrenset=$er6GBegrenset)"
    }
}

class GrunnlagInntektUføre(
    val år: Year,
    val inntektIKroner: Beløp,
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
        if (inntektIG != other.inntektIG) return false
        if (inntekt6GBegrenset != other.inntekt6GBegrenset) return false
        if (er6GBegrenset != other.er6GBegrenset) return false

        return true
    }

    override fun hashCode(): Int {
        var result = år.hashCode()
        result = 31 * result + inntektIKroner.hashCode()
        result = 31 * result + inntektIG.hashCode()
        result = 31 * result + inntekt6GBegrenset.hashCode()
        result = 31 * result + er6GBegrenset.hashCode()
        return result
    }

    override fun toString(): String {
        return "GrunnlagInntektUføre(år=$år, inntektIKroner=$inntektIKroner, inntektIG=$inntektIG, inntekt6GBegrenset=$inntekt6GBegrenset, er6GBegrenset=$er6GBegrenset)"
    }
}
