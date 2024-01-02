package no.nav.aap.behandlingsflyt.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.flyt.FlytKontekst

interface AvklaringsbehovsLøser<T : AvklaringsbehovLøsning> {

    fun løs(kontekst: FlytKontekst, løsning: T): LøsningsResultat

    fun forBehov(): Definisjon
}
