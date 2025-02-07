package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom

class SykdomGrunnlag(
    private val id: Long?,
    val yrkesskadevurdering: Yrkesskadevurdering?,
    val sykdomsvurderinger: List<Sykdomsvurdering>,
) {
    @Deprecated("kan være flere enn en sykdomsvurdering, bruk `sykdomsvurderinger`")
    val sykdomsvurdering: Sykdomsvurdering?
        get() = sykdomsvurderinger.firstOrNull()

    constructor(id: Long?, yrkesskadevurdering: Yrkesskadevurdering?, sykdomsvurdering: Sykdomsvurdering?):
            this(id, yrkesskadevurdering, listOfNotNull(sykdomsvurdering))

    fun erKonsistentForSykdom(harYrkesskadeRegistrert: Boolean): Boolean {
        return sykdomsvurdering?.erKonsistentForSykdom(harYrkesskadeRegistrert) ?: false
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SykdomGrunnlag

        if (yrkesskadevurdering != other.yrkesskadevurdering) return false
        if (sykdomsvurderinger != other.sykdomsvurderinger) return false

        return true
    }

    override fun hashCode(): Int {
        var result = yrkesskadevurdering?.hashCode() ?: 0
        result = 31 * result + sykdomsvurderinger.hashCode()
        return result
    }

    fun id(): Long {
        return requireNotNull(id)
    }

}
