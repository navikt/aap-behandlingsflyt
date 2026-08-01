package no.nav.aap.behandlingsflyt.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.avklaringsbehov.løsning.ForeslåUttakLøsning
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.lookup.repository.RepositoryProvider

class ForeslåUttakLøser() : AvklaringsbehovsLøser<ForeslåUttakLøsning> {
    constructor(repositoryProvider: RepositoryProvider): this()

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: ForeslåUttakLøsning): LøsningsResultat {
        // DO NOTHING 4 Now
        return LøsningsResultat("")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.FORESLÅ_UTTAK
    }
}
