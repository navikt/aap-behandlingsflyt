package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.behandlingsflyt.behandling.etannetsted.BehovForAvklaringer
import no.nav.aap.behandlingsflyt.behandling.etannetsted.EtAnnetSted
import no.nav.aap.behandlingsflyt.behandling.etannetsted.Institusjon
import no.nav.aap.behandlingsflyt.behandling.etannetsted.InstitusjonsOpphold
import no.nav.aap.behandlingsflyt.behandling.etannetsted.Soning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.flate.OppholdVurdering
import no.nav.aap.komponenter.tidslinje.Segment

object MapInstitusjonoppholdTilRegel {

    fun map(behovForAvklaringer: BehovForAvklaringer): List<EtAnnetSted> {
        return behovForAvklaringer.perioderTilVurdering.segmenter()
            .filterNot { it.verdi.helse == null && it.verdi.soning == null }
            .map { EtAnnetSted(it.periode, mapSoning(it), mapInstitusjon(it)) }
    }

    private fun mapInstitusjon(segment: Segment<InstitusjonsOpphold>): Institusjon? {
        val helse = segment.verdi.helse
        if (helse == null) {
            return null
        }
        return Institusjon(
            erPåInstitusjon = true,
            skalGiReduksjon = helse.vurdering == OppholdVurdering.AVSLÅTT,
            skalGiUmiddelbarReduksjon = helse.umiddelbarReduksjon
        )
    }

    private fun mapSoning(segment: Segment<InstitusjonsOpphold>): Soning? {
        val soning = segment.verdi.soning
        if (soning == null) {
            return null
        }
        return Soning(soner = true, girOpphør = soning.vurdering == OppholdVurdering.AVSLÅTT)
    }
}