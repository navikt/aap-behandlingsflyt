package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.BekreftTotalvurderingKlageLøsning
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.lookup.repository.RepositoryProvider

class BekreftTotalvurderingKlageLøser() : AvklaringsbehovsLøser<BekreftTotalvurderingKlageLøsning> {
    constructor(repositoryProvider: RepositoryProvider) : this()

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: BekreftTotalvurderingKlageLøsning): LøsningsResultat {
        // DO NOTHING 4 Now
        return LøsningsResultat("")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.BEKREFT_TOTALVURDERING_KLAGE
    }
}
