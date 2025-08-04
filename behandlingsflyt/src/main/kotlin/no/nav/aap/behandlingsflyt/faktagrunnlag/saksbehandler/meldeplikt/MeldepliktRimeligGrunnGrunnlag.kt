package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.RimeligGrunnVurdering.Companion.tidslinje
import no.nav.aap.komponenter.tidslinje.Tidslinje

data class MeldepliktRimeligGrunnGrunnlag(
    val vurderinger: List<RimeligGrunnVurdering>
) {
    fun tilTidslinje(): Tidslinje<RimeligGrunnVurdering.RimeligGrunnVurderingData> {
        return vurderinger.tidslinje()
    }
}
