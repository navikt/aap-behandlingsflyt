package no.nav.aap.behandlingsflyt.underveis.regler

import no.nav.aap.behandlingsflyt.Periode
import no.nav.aap.behandlingsflyt.tidslinje.Segment
import no.nav.aap.behandlingsflyt.tidslinje.SegmentSammenslåer

class SlåSammenVurderingerSammenslåer : SegmentSammenslåer<Vurdering, EnkelVurdering, Vurdering> {
    override fun sammenslå(
        periode: Periode,
        venstreSegment: Segment<Vurdering>?,
        høyreSegment: Segment<EnkelVurdering>?
    ): Segment<Vurdering> {
        return if (venstreSegment?.verdi == null && høyreSegment?.verdi != null) {
            var nyVurdering = Vurdering()
            nyVurdering = nyVurdering.leggTilVurdering(høyreSegment.verdi.vilkår, høyreSegment.verdi.utfall)
            Segment(periode, nyVurdering)
        } else {
            var vurdering = venstreSegment?.verdi
            if (høyreSegment?.verdi != null) {
                vurdering = vurdering?.leggTilVurdering(høyreSegment.verdi.vilkår, høyreSegment.verdi.utfall)
            }
            Segment(periode, vurdering)
        }
    }
}
