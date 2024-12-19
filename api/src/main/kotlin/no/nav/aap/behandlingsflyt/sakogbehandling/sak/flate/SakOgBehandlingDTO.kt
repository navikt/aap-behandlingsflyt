package no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate

data class SakOgBehandlingDTO(
    val personIdent: String,
    val saksnummer: String,
    val status: String,
    val sisteBehandlingStatus: String
)

data class NullableSakOgBehandlingDTO(
    val sakOgBehandlingDTO: SakOgBehandlingDTO?
)