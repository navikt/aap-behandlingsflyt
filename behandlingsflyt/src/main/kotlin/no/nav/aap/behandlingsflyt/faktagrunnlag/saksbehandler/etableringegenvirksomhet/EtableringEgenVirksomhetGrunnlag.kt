package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.gjeldendeVurderinger
import no.nav.aap.komponenter.tidslinje.Tidslinje

data class EtableringEgenVirksomhetGrunnlag(
    val vurderinger: List<EtableringEgenVirksomhetVurdering>
) {
    fun gjeldendeVurderingerSomTidslinje(): Tidslinje<EtableringEgenVirksomhetVurdering> {
        return vurderinger.gjeldendeVurderinger()
    }
}