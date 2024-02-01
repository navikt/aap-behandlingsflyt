package no.nav.aap.behandlingsflyt.faktagrunnlag.barn

class BarnGrunnlag(private val id: Long, val barn: List<Barn>) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BarnGrunnlag

        return barn == other.barn
    }

    override fun hashCode(): Int {
        return barn.hashCode()
    }
}
