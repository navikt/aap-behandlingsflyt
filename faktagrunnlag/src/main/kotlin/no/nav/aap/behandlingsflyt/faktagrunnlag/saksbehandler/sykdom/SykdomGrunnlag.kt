package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom

class SykdomGrunnlag(
    private val id: Long?,
    val yrkesskadevurdering: Yrkesskadevurdering?,
    val sykdomsvurdering: Sykdomsvurdering?
) {

    fun erKonsistentForSykdom(): Boolean {
        if (sykdomsvurdering == null) {
            return false
        }
        if (!sykdomsvurdering.harSkadeSykdomEllerLyte && sykdomsvurdering.erSkadeSykdomEllerLyteVesentligdel == true) {
            return false
        }
        if (sykdomsvurdering.erArbeidsevnenNedsatt == false && sykdomsvurdering.erNedsettelseIArbeidsevneMerEnnHalvparten == true) {
            return false
        }
        if (sykdomsvurdering.erNedsettelseIArbeidsevneMerEnnHalvparten != null &&
            !sykdomsvurdering.erNedsettelseIArbeidsevneMerEnnHalvparten &&
            sykdomsvurdering.erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense == null
        ) {
            return false
        }
        if (sykdomsvurdering.erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense != null && sykdomsvurdering.yrkesskadeBegrunnelse.isNullOrBlank()) {
            return false
        }
        return true
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SykdomGrunnlag

        if (yrkesskadevurdering != other.yrkesskadevurdering) return false
        if (sykdomsvurdering != other.sykdomsvurdering) return false

        return true
    }

    override fun hashCode(): Int {
        var result = yrkesskadevurdering?.hashCode() ?: 0
        result = 31 * result + (sykdomsvurdering?.hashCode() ?: 0)
        return result
    }

    fun id(): Long {
        return requireNotNull(id)
    }

}
