package no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt

import no.nav.aap.komponenter.tidslinje.StandardSammenslåere
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tid
import java.util.Objects

data class Aktivitetsplikt11_7Grunnlag(
    val vurderinger: List<Aktivitetsplikt11_7Vurdering>
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Aktivitetsplikt11_7Grunnlag

        return vurderinger.toSet() == other.vurderinger.toSet()
    }

    override fun hashCode(): Int {
        return Objects.hash(vurderinger.toSet())
    }

    fun tidslinje(): Tidslinje<Aktivitetsplikt11_7Vurdering> {
        return vurderinger
            .sortedBy { it.gjelderFra }
            .fold(Tidslinje()) { tidslinje, vurdering ->
                tidslinje.kombiner(
                    Tidslinje(
                        Periode(vurdering.gjelderFra, Tid.MAKS), vurdering
                    ),
                    StandardSammenslåere.prioriterHøyreSideCrossJoin()
                )
            }
    }
}