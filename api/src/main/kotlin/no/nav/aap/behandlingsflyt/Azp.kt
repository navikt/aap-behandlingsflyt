package no.nav.aap.behandlingsflyt

import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.tilgang.AuthorizationMachineToMachineConfig
import java.util.UUID
import kotlin.collections.plus

enum class Azp(val uuid: UUID) {
    Brev(UUID.fromString(requiredConfigForKey("INTEGRASJON_BREV_AZP"))),
    Tilgang(UUID.fromString(requiredConfigForKey("INTEGRASJON_TILGANG_AZP"))),
    Postmottak(UUID.fromString(requiredConfigForKey("INTEGRASJON_POSTMOTTAK_AZP"))),
    Dokumentinnhenting(UUID.fromString(requiredConfigForKey("INTEGRASJON_DOKUMENTINNHENTING_AZP"))),
    Saksbehandling(UUID.fromString(requiredConfigForKey("INTEGRASJON_SAKSBEHANDLING_AZP"))),
}

fun AuthorizationMachineToMachineConfig.medAzureTokenGen(): AuthorizationMachineToMachineConfig {
    if (!Miljø.erDev()) return this
    return this.copy(
        authorizedAzps = this.authorizedAzps
                + UUID.fromString(requiredConfigForKey("INTEGRASJON_AZURE_TOKEN_GENERATOR_AZP"))
    )
}
      