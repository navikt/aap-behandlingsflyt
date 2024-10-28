package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.dokumentinnhenting

import java.util.*

data class LegeerklæringBestillingRequest (
    val behandlerRef: String,
    val personIdent: String,
    val dialogmeldingTekst: String,
    val sakId: String,
    val dokumentasjonType: DokumentasjonType,
    val dialogmeldingVedlegg: ByteArray?
)

data class LegeerklæringBestillingResponse(
    val dialogmeldingUUID: String
)

data class LegeerklæringStatusResponse(
    val dialogmeldingUuid: UUID,
    val status: MeldingStatusType?,
    val behandlerRef: String,
    val personId: String,
    val sakId: String,
)

enum class DokumentasjonType {
    L40, L8, L120, MELDING_FRA_NAV, RETUR_LEGEERKLÆRING
}

enum class MeldingStatusType {
    BESTILT, SENDT, OK, AVVIST
}