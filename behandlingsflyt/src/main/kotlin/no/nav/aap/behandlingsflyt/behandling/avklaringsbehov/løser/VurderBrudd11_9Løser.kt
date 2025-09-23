package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.VurderBrudd11_9Løsning
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.lookup.repository.RepositoryProvider

class VurderBrudd11_9Løser() : AvklaringsbehovsLøser<VurderBrudd11_9Løsning> {
    constructor(repositoryProvider: RepositoryProvider): this()

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: VurderBrudd11_9Løsning): LøsningsResultat {
        // TODO: Implementer løsning
        return LøsningsResultat("Fullført")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.VURDER_BRUDD_11_9
    }
}
