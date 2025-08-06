package no.nav.aap.behandlingsflyt

import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.tilgang.AuthorizationMachineToMachineConfig
import java.util.UUID
import kotlin.collections.plus

enum class Azp(val uuid: UUID) {
    Brev(UUID.fromString(requiredConfigForKey("integrasjon.brev.azp"))),
    Tilgang(UUID.fromString(requiredConfigForKey("integrasjon.tilgang.azp"))),
    Postmottak(UUID.fromString(requiredConfigForKey("integrasjon.postmottak.azp"))),
    Dokumentinnhenting(UUID.fromString(requiredConfigForKey("integrasjon.dokumentinnhenting.azp"))),
    Saksbehandling(UUID.fromString(requiredConfigForKey("integrasjon.saksbehandling.azp"))),
}

fun AuthorizationMachineToMachineConfig.medAzureTokenGen(): AuthorizationMachineToMachineConfig {
    if (!Miljø.erDev()) return this
    return this.copy(
        authorizedAzps = this.authorizedAzps
                + UUID.fromString(requiredConfigForKey("integrasjon.azure.token.generator.azp"))
    )
}
      