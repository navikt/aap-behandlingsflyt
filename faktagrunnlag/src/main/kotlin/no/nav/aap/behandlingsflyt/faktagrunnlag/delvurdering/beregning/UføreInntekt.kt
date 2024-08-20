package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning

import no.nav.aap.verdityper.Beløp
import no.nav.aap.verdityper.Prosent
import java.time.Year

/**
 * @param år Hvilket år inntekten gjelder for.
 * @param inntektIKroner Inntekt i kroner for dette året.
 * @param uføregrad Uføregrad i prosent.
 * @param arbeidsgrad Komplement av uføregrad.
 * @param inntektJustertForUføregrad Inntekter oppjustert for uføregrad etter §11-28, fjerde ledd.
 */
class UføreInntekt(
    val år: Year,
    val inntektIKroner: Beløp,
    val uføregrad: Prosent,
    val arbeidsgrad: Prosent,
    val inntektJustertForUføregrad: Beløp
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UføreInntekt

        if (år != other.år) return false
        if (inntektIKroner != other.inntektIKroner) return false
        if (uføregrad != other.uføregrad) return false
        if (arbeidsgrad != other.arbeidsgrad) return false
        if (inntektJustertForUføregrad != other.inntektJustertForUføregrad) return false

        return true
    }

    override fun hashCode(): Int {
        var result = år.hashCode()
        result = 31 * result + inntektIKroner.hashCode()
        result = 31 * result + uføregrad.hashCode()
        result = 31 * result + arbeidsgrad.hashCode()
        result = 31 * result + inntektJustertForUføregrad.hashCode()
        return result
    }

    override fun toString(): String {
        return "UføreInntekt(år=$år, inntektIKroner=$inntektIKroner, uføregrad=$uføregrad, arbeidsgrad=$arbeidsgrad, inntektJustertForUføregrad=$inntektJustertForUføregrad)"
    }
}
