package no.nav.aap.behandlingsflyt.integrasjon.organisasjon

import java.time.LocalDate

data class NomData(
    val ressurs: NomDataRessurs?,
)

data class NomDataRessurs(
    val orgTilknytning: List<OrgTilknytning>,
    val visningsnavn: String,
)

data class OrgTilknytning(
    val orgEnhet: OrgEnhet,
    val erDagligOppfolging: Boolean,
    val gyldigFom: LocalDate,
    val gyldigTom: LocalDate?,
)

data class OrgEnhet(val remedyEnhetId: String?)

data class NomRessurserVisningsnavn(
    val ressurser: List<NomRessursResponse>,
)

data class NomRessursResponse(
    val ressurs: NomRessursVisningsnavn
)

data class NomRessursVisningsnavn(
    val navident: String,
    val visningsnavn: String,
)
