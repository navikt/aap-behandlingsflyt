package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangarbeid

import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import java.time.LocalDate

data class OvergangArbeidGrunnlag(
    val id: Long?,
    val vurderinger: List<OvergangArbeidVurdering>,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OvergangArbeidGrunnlag

        return vurderinger == other.vurderinger
    }

    override fun hashCode(): Int {
        return vurderinger.hashCode()
    }

    fun somOvergangArbeidvurderingstidslinje(startDato: LocalDate): Tidslinje<OvergangArbeidGrunnlag> {
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