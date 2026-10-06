package no.nav.aap.meldeplikt

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.gjeldendeVurderinger
import no.nav.aap.meldeplikt.Fritaksvurdering.FritaksvurderingData
import no.nav.aap.komponenter.tidslinje.Tidslinje

data class MeldepliktGrunnlag(
    val vurderinger: List<Fritaksvurdering>
) {
    fun gjeldendeVurderinger() = vurderinger.gjeldendeVurderinger()

    fun tilTidslinje(): Tidslinje<FritaksvurderingData> =
        vurderinger.gjeldendeVurderinger()
            .map { it.toFritaksvurderingData() }
            .komprimer()
}