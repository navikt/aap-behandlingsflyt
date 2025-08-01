package no.nav.aap.behandlingsflyt

import no.nav.aap.komponenter.config.configForKey
import no.nav.aap.komponenter.config.requiredConfigForKey
import java.util.UUID

enum class Azp(val uuid: UUID) {
    Brev(UUID.fromString(requiredConfigForKey("integrasjon.brev.azp"))),
    Tilgang(UUID.fromString(requiredConfigForKey("integrasjon.tilgang.azp"))),
    Postmottak(UUID.fromString(requiredConfigForKey("integrasjon.postmottak.azp"))),
    Dokumentinnhenting(UUID.fromString(requiredConfigForKey("integrasjon.dokumentinnhenting.azp"))),
    Saksbehandling(UUID.fromString(requiredConfigForKey("integrasjon.saksbehandling.azp"))),
}