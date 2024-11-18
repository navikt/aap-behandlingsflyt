package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.tidslinje.Segment

internal class SlåSammenVurderingerSammenslåer {
    internal fun sammenslå(
        periode: Periode,
        venstreSegment: Segment<Vurdering>?,
        høyreSegment: Segment<EnkelVurdering>?
    ): Segment<Vurdering> {
        val vurdering = venstreSegment?.verdi ?: Vurdering()
        if (høyreSegment != null) {
            return Segment(periode, vurdering.leggTilVurdering(høyreSegment.verdi.vilkår, høyreSegment.verdi.utfall))
        }
        return Segment(periode, vurdering)
    }
}
