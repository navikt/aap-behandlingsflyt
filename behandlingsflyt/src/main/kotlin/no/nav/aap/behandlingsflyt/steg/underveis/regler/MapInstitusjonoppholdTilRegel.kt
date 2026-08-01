package no.nav.aap.behandlingsflyt.steg.underveis.regler

import no.nav.aap.behandlingsflyt.steg.institusjon.BehovForAvklaringer
import no.nav.aap.institusjonsopphold.Institusjon
import no.nav.aap.institusjonsopphold.Institusjonsopphold
import no.nav.aap.behandlingsflyt.steg.institusjon.InstitusjonsoppholdVurdering
import no.nav.aap.institusjonsopphold.Soning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.flate.OppholdVurdering
import no.nav.aap.komponenter.tidslinje.Segment

object MapInstitusjonoppholdTilRegel {

    fun map(behovForAvklaringer: BehovForAvklaringer): List<Institusjonsopphold> {
        return behovForAvklaringer.perioderTilVurdering.segmenter()
            .filterNot { it.verdi.helse == null && it.verdi.soning == null }
            .map { Institusjonsopphold(it.periode, mapSoning(it), mapInstitusjon(it)) }
    }

    private fun mapInstitusjon(segment: Segment<InstitusjonsoppholdVurdering>): Institusjon? {
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

    private fun mapSoning(segment: Segment<InstitusjonsoppholdVurdering>): Soning? {
        val soning = segment.verdi.soning ?: return null
        return Soning(soner = true, girOpphør = soning.vurdering == OppholdVurdering.AVSLÅTT)
    }
}