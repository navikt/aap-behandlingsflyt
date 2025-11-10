package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand

import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import java.time.LocalDate

data class BistandGrunnlag(
    val vurderinger: List<BistandVurdering>,
) {
    fun harVurdertPeriode(periode: Periode): Boolean {
        return true
    }

    fun somBistandsvurderingstidslinje(startDato: LocalDate): Tidslinje<BistandVurdering> {
        return vurderinger
            .sortedBy { it.vurderingenGjelderFra ?: startDato }
            .fold(Tidslinje()) { tidslinje, vurdering ->
                tidslinje.kombiner(
                    Tidslinje(Periode(vurdering.vurderingenGjelderFra ?: startDato, Tid.MAKS), vurdering),
                    StandardSammenslåere.prioriterHøyreSideCrossJoin()
                )
            }
    }
}
