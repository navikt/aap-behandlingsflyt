package no.nav.aap.behandlingsflyt.arena

import java.time.LocalDate

data class SakerRequest(
    val personidentifikator: String
)

data class SakerResponse(
    val saker: List<SakOppsummering>
)

data class SakOppsummering(
    val sakId: String,
    val lopenummer: Int,
    val aar: Int,
    val antallVedtak: Int,
    val statuskode: String,
    val statusnavn: String,
    val sakstype: String?,
    val regDato: LocalDate,
    val avsluttetDato: LocalDate?,
)