package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom

class SykepengerErstatningGrunnlag(
    val id: Long? = null,
    val vurdering: SykepengerVurdering?
) {
    fun vurdering(): SykepengerVurdering? {
        return vurdering
    }

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
