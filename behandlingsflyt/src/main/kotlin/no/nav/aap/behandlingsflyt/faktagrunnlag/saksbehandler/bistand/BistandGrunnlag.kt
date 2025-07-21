package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand

import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import java.time.LocalDate

class BistandGrunnlag(
    val id: Long?,
    val vurderinger: List<BistandVurdering>,
) {
    fun harVurdertPeriode(periode: Periode): Boolean {
        return true
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BistandGrunnlag

        return vurderinger == other.vurderinger
    }

    override fun hashCode(): Int {
        return vurderinger.hashCode()
    }
    
    fun somBistandsvurderingstidslinje(startDato: LocalDate): Tidslinje<BistandVurdering> {
        return vurderinger
            .sortedBy { it.vurderingenGjelderFra ?: startDato }
            .fold(Tidslinje()) { tidslinje, vurdering ->
                tidslinje.kombiner(
                    Tidslinje(Periode(vurdering.vurderingenGjelderFra ?: startDato, LocalDate.MAX), vurdering),
                    StandardSammenslåere.prioriterHøyreSideCrossJoin()
                )
            }
    }
}
