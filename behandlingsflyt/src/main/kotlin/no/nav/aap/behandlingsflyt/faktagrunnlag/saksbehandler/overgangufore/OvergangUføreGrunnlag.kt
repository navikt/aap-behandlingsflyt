package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore

import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import java.time.LocalDate

data class OvergangUføreGrunnlag(
    val id: Long?,
    val vurderinger: List<OvergangUføreVurdering>,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OvergangUføreGrunnlag

        return vurderinger == other.vurderinger
    }

    override fun hashCode(): Int {
        return vurderinger.hashCode()
    }
    
    fun somOvergangUforevurderingstidslinje(startDato: LocalDate): Tidslinje<OvergangUføreVurdering> {
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
