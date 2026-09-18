package no.nav.aap.behandlingsflyt.arena

data class HarHistorikkRequest(
    val personidentifikator: String,
)

data class HarHistorikkResponse(
    val harHistorikk: Boolean,
)
