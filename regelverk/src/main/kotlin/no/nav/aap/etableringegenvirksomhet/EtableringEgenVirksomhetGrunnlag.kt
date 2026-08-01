package no.nav.aap.etableringegenvirksomhet

import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.misc.gjeldendeVurderinger

data class EtableringEgenVirksomhetGrunnlag(
    val vurderinger: List<EtableringEgenVirksomhetVurdering>
) {
    fun gjeldendeVurderingerSomTidslinje(): Tidslinje<EtableringEgenVirksomhetVurdering> {
        return vurderinger.gjeldendeVurderinger()
    }
}