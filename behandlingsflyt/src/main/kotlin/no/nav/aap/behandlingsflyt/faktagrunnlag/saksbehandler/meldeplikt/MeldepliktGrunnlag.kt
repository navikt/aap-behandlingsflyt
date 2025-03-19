package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.Fritaksvurdering.Companion.tidslinje
import no.nav.aap.komponenter.tidslinje.Tidslinje

data class MeldepliktGrunnlag(
    val vurderinger: List<Fritaksvurdering>
) {
    fun tilTidslinje(): Tidslinje<Fritaksvurdering.FritaksvurderingData> {
        return vurderinger.tidslinje()
    }
}
