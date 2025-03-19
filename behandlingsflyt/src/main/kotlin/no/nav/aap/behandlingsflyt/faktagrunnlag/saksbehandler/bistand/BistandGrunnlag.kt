package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand

import no.nav.aap.komponenter.type.Periode

class BistandGrunnlag(
    val id: Long?,
    val vurdering: BistandVurdering,
) {
    fun harVurdertPeriode(periode: Periode): Boolean {
        return true
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BistandGrunnlag

        if (vurdering != other.vurdering) return false

        return true
    }
}
