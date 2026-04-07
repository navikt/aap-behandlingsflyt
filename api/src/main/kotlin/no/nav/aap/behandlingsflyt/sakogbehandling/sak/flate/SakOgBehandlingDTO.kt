package no.nav.aap.behandlingsflyt.sakogbehandling.sak.flate

data class SakOgBehandlingDTO(
    val saksnummer: String
)

data class NullableSakOgBehandlingDTO(
    val sakOgBehandlingDTO: SakOgBehandlingDTO?
)