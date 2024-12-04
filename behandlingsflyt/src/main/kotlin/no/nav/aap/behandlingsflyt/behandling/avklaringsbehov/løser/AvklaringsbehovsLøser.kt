package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklaringsbehovLøsning
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon

sealed interface AvklaringsbehovsLøser<in T : AvklaringsbehovLøsning> {

    fun løs(kontekst: AvklaringsbehovKontekst, løsning: T): LøsningsResultat

    fun forBehov(): Definisjon
}
