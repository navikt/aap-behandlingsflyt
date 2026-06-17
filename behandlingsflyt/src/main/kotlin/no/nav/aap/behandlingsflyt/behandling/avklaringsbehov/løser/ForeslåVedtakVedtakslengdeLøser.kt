package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.ForeslåVedtakVedtakslengdeLøsning
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.lookup.repository.RepositoryProvider

class ForeslåVedtakVedtakslengdeLøser() : AvklaringsbehovsLøser<ForeslåVedtakVedtakslengdeLøsning> {
    constructor(repositoryProvider: RepositoryProvider): this()

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: ForeslåVedtakVedtakslengdeLøsning): LøsningsResultat {
        // DO NOTHING 4 Now
        return LøsningsResultat("")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.FORESLÅ_VEDTAK_VEDTAKSLENGDE
    }
}

