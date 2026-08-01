package no.nav.aap.behandlingsflyt.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.avklaringsbehov.løsning.LegeerklæringBestillingLøsning
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.lookup.repository.RepositoryProvider

class LegeerklæringBestillingLøser() : AvklaringsbehovsLøser<LegeerklæringBestillingLøsning> {
    constructor(repositoryProvider: RepositoryProvider) : this()

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: LegeerklæringBestillingLøsning): LøsningsResultat {
        return LøsningsResultat("Tatt av vent (ventet på bestilling av legeerklæring)")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.BESTILL_LEGEERKLÆRING
    }
}