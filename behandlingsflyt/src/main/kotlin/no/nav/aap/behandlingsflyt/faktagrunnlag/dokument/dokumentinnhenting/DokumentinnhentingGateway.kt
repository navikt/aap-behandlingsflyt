package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.dokumentinnhenting

import no.nav.aap.lookup.gateway.Gateway

interface DokumentinnhentingGateway : Gateway {
    fun bestillLegeerklæring(request: LegeerklæringBestillingRequest): String
    fun purrPåLegeerklæring(purringRequest: LegeerklæringPurringRequest): String
    fun legeerklæringStatus(saksnummer: String): List<LegeerklæringStatusResponse>
    fun forhåndsvisBrev(request: BrevRequest): BrevResponse
}