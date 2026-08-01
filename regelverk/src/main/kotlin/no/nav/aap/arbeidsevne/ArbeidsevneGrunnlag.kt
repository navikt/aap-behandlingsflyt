package no.nav.aap.arbeidsevne

import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.misc.gjeldendeVurderinger

data class ArbeidsevneGrunnlag(
    val vurderinger: List<ArbeidsevneVurdering>,
) {
    fun gjeldendeVurderinger() = vurderinger.gjeldendeVurderinger()

    fun tilTidslinje(): Tidslinje<ArbeidsevneVurdering.ArbeidsevneVurderingData> =
        vurderinger.gjeldendeVurderinger()
            .map { it.toArbeidsevneVurderingData() }
            .komprimer()
}