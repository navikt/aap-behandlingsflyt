package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom

import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import java.time.LocalDate

data class SykdomGrunnlag(
    val yrkesskadevurdering: Yrkesskadevurdering?,
    val sykdomsvurderinger: List<Sykdomsvurdering>,
) {

    fun somSykdomsvurderingstidslinje(startDato: LocalDate): Tidslinje<Sykdomsvurdering> {
        return sykdomsvurderinger
            .sortedBy { it.vurderingenGjelderFra ?: startDato }
            .fold(Tidslinje()) { tidslinje, vurdering ->
                tidslinje.kombiner(
                    Tidslinje(Periode(vurdering.vurderingenGjelderFra ?: startDato, Tid.MAKS), vurdering),
                    StandardSammenslåere.prioriterHøyreSideCrossJoin()
                )
            }
    }
}
