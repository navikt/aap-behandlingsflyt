package no.nav.aap.behandlingsflyt.behandling.institusjonsopphold

import no.nav.aap.komponenter.tidslinje.Tidslinje

class BehovForAvklaringer(
    val perioderTilVurdering: Tidslinje<InstitusjonsoppholdVurdering>
) {

    fun harBehovForAvklaring(): Boolean {
        return perioderTilVurdering.segmenter().any { it.verdi.harNoeUavklart() }
    }

}
