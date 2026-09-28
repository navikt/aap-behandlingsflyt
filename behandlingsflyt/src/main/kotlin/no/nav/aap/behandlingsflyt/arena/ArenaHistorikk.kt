package no.nav.aap.behandlingsflyt.arena

data class HarArenaHistorikkRequest(
    val personidentifikator: String,
)

data class HarArenaHistorikkResponse(
    val harHistorikk: Boolean,
)
