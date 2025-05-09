package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.UtenlandskVidereføringLøsning
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.lookup.repository.RepositoryProvider

class UtenlandskVidereføringLøser() : AvklaringsbehovsLøser<UtenlandskVidereføringLøsning> {

    constructor(repositoryProvider: RepositoryProvider) : this()

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: UtenlandskVidereføringLøsning): LøsningsResultat {
        // TODO: Kreve at de legger inn ny data her, denne vil låse seg selv igjen til ny manuell vurdering kommer inn
        return LøsningsResultat("Tatt av vent (ventet på utenlandsk videreføring)")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.VENTE_PÅ_UTENLANDSK_VIDEREFØRING_AVKLARING
    }
}