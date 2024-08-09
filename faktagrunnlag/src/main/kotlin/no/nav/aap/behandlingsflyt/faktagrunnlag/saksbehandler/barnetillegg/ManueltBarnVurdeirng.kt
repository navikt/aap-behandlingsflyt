package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barnetillegg

import no.nav.aap.verdityper.Periode
import no.nav.aap.verdityper.sakogbehandling.Ident

class ManueltBarnVurdeirng(
    val ident: Ident,
    val begrunnelse: String,
    val skalBeregnesBarnetillegg: Boolean,
    val perioder: List<Periode>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ManueltBarnVurdeirng

        if (ident != other.ident) return false
        if (perioder != other.perioder) return false

        return true
    }

    override fun hashCode(): Int {
        var result = ident.hashCode()
        result = 31 * result + perioder.hashCode()
        return result
    }
}