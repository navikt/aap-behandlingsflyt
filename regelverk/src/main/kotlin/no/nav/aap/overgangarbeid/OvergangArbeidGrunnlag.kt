package no.nav.aap.overgangarbeid

import no.nav.aap.misc.gjeldendeVurderinger
import no.nav.aap.komponenter.tidslinje.Tidslinje

data class OvergangArbeidGrunnlag(
    val vurderinger: List<OvergangArbeidVurdering>,
) {
    fun gjeldendeVurderinger(): Tidslinje<OvergangArbeidVurdering> {
        return vurderinger.gjeldendeVurderinger()
    }
}