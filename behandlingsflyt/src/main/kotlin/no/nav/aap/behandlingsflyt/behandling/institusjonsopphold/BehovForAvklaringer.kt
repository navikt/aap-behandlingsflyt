package no.nav.aap.behandlingsflyt.behandling.institusjonsopphold

import no.nav.aap.komponenter.tidslinje.Tidslinje

class BehovForAvklaringer(
    val perioderTilVurdering: Tidslinje<InstitusjonsoppholdVurdering>,
    val barneTilleggDekkerHelePerioden: Boolean,
    val forKortOpphold: Boolean
) {

    fun harBehovForAvklaring(): Boolean {
        return perioderTilVurdering.segmenter().any { it.verdi.harNoeUavklart() }
    }
}
