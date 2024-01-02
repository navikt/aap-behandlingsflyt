package no.nav.aap.behandlingsflyt.faktagrunnlag.sykdom

import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.sykdom.SykepengerVurdering

class SykepengerErstatningGrunnlag(
    val id: Long? = null,
    val vurdering: SykepengerVurdering?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SykepengerErstatningGrunnlag

        return vurdering == other.vurdering
    }

    override fun hashCode(): Int {
        return vurdering?.hashCode() ?: 0
    }
}
