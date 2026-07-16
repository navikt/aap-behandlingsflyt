package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.gjeldendeVurderinger
import no.nav.aap.komponenter.tidslinje.Tidslinje

data class ArbeidsevneGrunnlag(
    val vurderinger: List<ArbeidsevneVurdering>,
) {
    fun gjeldendeVurderinger() = vurderinger.gjeldendeVurderinger()

    fun tilTidslinje(): Tidslinje<ArbeidsevneVurdering.ArbeidsevneVurderingData> =
        vurderinger.gjeldendeVurderinger()
            .map { it.toArbeidsevneVurderingData() }
            .komprimer()
}
