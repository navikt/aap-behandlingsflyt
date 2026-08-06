package no.nav.aap.behandlingsflyt.hendelse.datadeling

import java.time.LocalDate

data class ArenaSakOppsummering(
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

data class ArenaSakerRequest(
    val personidentifikator: String,
)

data class ArenaSakerResponse(
    val saker: List<ArenaSakOppsummering>,
)
