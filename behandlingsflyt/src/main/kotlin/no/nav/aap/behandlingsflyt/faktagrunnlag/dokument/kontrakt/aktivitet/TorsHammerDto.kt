package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.kontrakt.aktivitet

import java.time.LocalDate

class TorsHammerDto(
    val saksnummer: String,
    val hammer: HammerDto
)

enum class HammerType {
    IKKE_MØTT_TIL_MØTE,
    IKKE_MØTT_TIL_BEHANDLING,
    IKKE_MØTT_TIL_TILTAK,
    IKKE_MØTT_TIL_ANNEN_AKTIVITET,
    IKKE_SENDT_INN_DOKUMENTASJON,
    IKKE_AKTIVT_BIDRAG
}

class HammerDto(
    val type: HammerType,
    val dato: LocalDate,
    val begrunnelse: String
)
