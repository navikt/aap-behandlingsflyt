package no.nav.aap.meldeplikt

import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.misc.gjeldendeVurderinger

data class MeldepliktGrunnlag(
    val vurderinger: List<Fritaksvurdering>
) {
    fun gjeldendeVurderinger() = vurderinger.gjeldendeVurderinger()

    fun tilTidslinje(): Tidslinje<Fritaksvurdering.FritaksvurderingData> =
        vurderinger.gjeldendeVurderinger()
            .map { it.toFritaksvurderingData() }
            .komprimer()
}