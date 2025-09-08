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
    constructor(id: Long?, yrkesskadevurdering: Yrkesskadevurdering?, sykdomsvurdering: Sykdomsvurdering?) :
            this(id, yrkesskadevurdering, listOfNotNull(sykdomsvurdering))


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
        if (sykdomsvurderinger.toSet() != other.sykdomsvurderinger.toSet()) return false

        return true
    }

    override fun hashCode(): Int {
        var result = yrkesskadevurdering?.hashCode() ?: 0
        result = 31 * result + sykdomsvurderinger.toSet().hashCode()
        return result
    }

    fun id(): Long {
        return requireNotNull(id)
    }

}
