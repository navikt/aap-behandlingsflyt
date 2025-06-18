package no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.refusjon

data class NavEnheterResponse(
    val navn: String,
    val enhetsnummer: String,
)

data class NavEnheterRequest(
    val navn: String,
)