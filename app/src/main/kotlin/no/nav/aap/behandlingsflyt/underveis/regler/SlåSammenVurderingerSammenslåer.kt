package no.nav.aap.behandlingsflyt.underveis.regler

import no.nav.aap.tidslinje.Segment
import no.nav.aap.tidslinje.SegmentSammenslåer
import no.nav.aap.verdityper.Periode

class SlåSammenVurderingerSammenslåer : SegmentSammenslåer<Vurdering, EnkelVurdering, Vurdering> {
    override fun sammenslå(
        periode: Periode,
        venstreSegment: Segment<Vurdering>?,
        høyreSegment: Segment<EnkelVurdering>?
    ): Segment<Vurdering> {
        val høyreSegmentVerdi = høyreSegment?.verdi
        val venstreSegmentVerdi = venstreSegment?.verdi

        return if (venstreSegmentVerdi == null && høyreSegmentVerdi != null) {
            var nyVurdering = Vurdering()
            nyVurdering = nyVurdering.leggTilVurdering(høyreSegmentVerdi.vilkår, høyreSegmentVerdi.utfall)
            Segment(periode, nyVurdering)
        } else {
            var vurdering = venstreSegmentVerdi
            if (høyreSegmentVerdi != null) {
                vurdering = vurdering?.leggTilVurdering(høyreSegmentVerdi.vilkår, høyreSegmentVerdi.utfall)
            }
            Segment(periode, vurdering)
        }
    }
}
