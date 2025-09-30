package no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt

import java.util.Objects

data class Aktivitetsplikt11_9Grunnlag(
    val vurderinger: List<Aktivitetsplikt11_9Vurdering>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Aktivitetsplikt11_9Grunnlag

        if (vurderinger.toSet() != other.vurderinger.toSet()) return false

        return true
    }

    override fun hashCode(): Int {
        return Objects.hash(vurderinger.toSet())
    }
}