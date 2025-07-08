package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.UtenlandskVidereføringLøsning
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.lookup.repository.RepositoryProvider

class UtenlandskVidereføringLøser() : AvklaringsbehovsLøser<UtenlandskVidereføringLøsning> {

    constructor(repositoryProvider: RepositoryProvider) : this()

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: UtenlandskVidereføringLøsning): LøsningsResultat {
        // TODO: Handler om utenlandsk lovvalg, legg inn når vi har støtte for dette
        return LøsningsResultat("Tatt av vent (ventet på utenlandsk videreføring)")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.VENTE_PÅ_UTENLANDSK_VIDEREFØRING_AVKLARING
    }
}