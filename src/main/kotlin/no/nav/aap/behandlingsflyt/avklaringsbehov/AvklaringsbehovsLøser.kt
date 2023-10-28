package no.nav.aap.behandlingsflyt.avklaringsbehov

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.flyt.FlytKontekst

interface AvklaringsbehovsLøser<T : AvklaringsbehovLøsning> {

    fun løs(kontekst: FlytKontekst, løsning: T): LøsningsResultat

    fun forBehov(): Definisjon
}
