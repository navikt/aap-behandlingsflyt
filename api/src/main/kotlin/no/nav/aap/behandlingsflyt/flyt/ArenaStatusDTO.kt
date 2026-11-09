package no.nav.aap.behandlingsflyt.flyt

@Deprecated("Bruk harArenaHistorikk fra GET /api/behandling/{referanse}/egenskaper")
data class ArenaStatusDTO(val harArenaHistorikk: Boolean)
