package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.dokumentinnhenting

import java.time.LocalDateTime
import java.util.*

data class LegeerklæringBestillingRequest (
    val behandlerRef: String,
    val behandlerNavn: String,
    val behandlerHprNr: String,
    val veilederNavn: String,
    val personIdent: String,
    val personNavn: String,
    val dialogmeldingTekst: String,
    val saksnummer: String,
    val dokumentasjonType: DokumentasjonType,
    val behandlingsReferanse: UUID
)

data class LegeerklæringPurringRequest(
    val dialogmeldingUuid: UUID
)

data class LegeerklæringStatusResponse(
    val dialogmeldingUuid: UUID,
    val status: MeldingStatusType?,
    val statusTekst: String?,
    val behandlerRef: String,
    val behandlerNavn: String,
    val personId: String,
    val saksnummer: String,
    val opprettet: LocalDateTime,
    val fritekst: String
)

data class BrevRequest(
    val personNavn: String,
    val personIdent: String,
    val dialogmeldingTekst: String,
    val veilederNavn: String,
    val dokumentasjonType: DokumentasjonType
)

data class BrevResponse(
    val konstruertBrev: String
)

enum class DokumentasjonType {
    L40, L8, L120, MELDING_FRA_NAV, RETUR_LEGEERKLÆRING, PURRING
}

enum class MeldingStatusType {
    BESTILT, SENDT, OK, AVVIST
}

