package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.dokumentinnhenting

import java.util.*

data class LegeerklæringBestillingRequest (
    val behandlerRef: String,
    val personIdent: String,
    val dialogmeldingType: String,
    val dialogmeldingKodeverk: String,
    val dialogmeldingKode: Int,
    val dialogmeldingTekst: String,
    val dialogmeldingVedlegg: ByteArray?,
    val sakId: String
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

enum class MeldingStatusType {
    BESTILT, SENDT, OK, AVVIST
}