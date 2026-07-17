package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangarbeid

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.gjeldendeVurderinger
import no.nav.aap.komponenter.tidslinje.Tidslinje

data class OvergangArbeidGrunnlag(
    val vurderinger: List<OvergangArbeidVurdering>,
) {
    fun gjeldendeVurderinger(): Tidslinje<OvergangArbeidVurdering> {
        return vurderinger.gjeldendeVurderinger()
    }
}