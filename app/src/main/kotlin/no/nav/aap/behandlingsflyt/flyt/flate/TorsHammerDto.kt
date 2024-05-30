package no.nav.aap.behandlingsflyt.flyt.flate

import java.time.LocalDate

class TorsHammerDto(
    val saksnummer: String,
    val hammer: HammerDto
)

enum class HammerType {
    IKKE_MØTT_TIL_MØTE, IKKE_MØTT_TIL_BEHANDLING, IKKE_MØTT_TIL_KLAGE, IKKE_SENDT_INN_DOKUMENTASJON
}

class HammerDto(
    val type: HammerType,
    val dato: LocalDate,
)
