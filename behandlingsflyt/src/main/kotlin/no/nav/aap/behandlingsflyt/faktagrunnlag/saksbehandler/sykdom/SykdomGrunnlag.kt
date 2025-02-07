package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom

import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import java.time.LocalDate

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

    fun somSykdomsvurderingstidslinje(startDato: LocalDate): Tidslinje<Sykdomsvurdering> {
        return sykdomsvurderinger
            .sortedBy { it.vurderingenGjelderFra ?: startDato }
            .fold(Tidslinje()) { tidslinje, vurdering ->
                tidslinje.kombiner(
                    Tidslinje(Periode(vurdering.vurderingenGjelderFra ?: startDato, LocalDate.MAX), vurdering),
                    StandardSammenslåere.prioriterHøyreSideCrossJoin()
                )
            }
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
