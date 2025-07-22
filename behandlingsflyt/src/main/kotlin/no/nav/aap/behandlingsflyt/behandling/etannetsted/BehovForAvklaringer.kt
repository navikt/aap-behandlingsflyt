package no.nav.aap.behandlingsflyt.behandling.etannetsted

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.flate.OppholdVurdering
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.komponenter.tidslinje.Tidslinje

class BehovForAvklaringer(
    val perioderTilVurdering: Tidslinje<InstitusjonsOpphold>
) {

    fun harBehovForAvklaring(): Boolean {
        return perioderTilVurdering.segmenter().any { it.verdi.harNoeUavklart() }
    }

    fun avklaringsbehov(): List<Definisjon> {
        if (!harBehovForAvklaring()) {
            return emptyList()
        }
        if (perioderTilVurdering.segmenter().any { it.verdi.soning?.vurdering == OppholdVurdering.UAVKLART }) {
            return listOf(Definisjon.AVKLAR_SONINGSFORRHOLD)
        }
        return listOf(Definisjon.AVKLAR_HELSEINSTITUSJON)
    }

}