package no.nav.aap.behandlingsflyt.integrasjon.dokumentinnhenting

import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.dokumentinnhenting.BrevRequest
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.dokumentinnhenting.BrevResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.dokumentinnhenting.DokumentinnhentingGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.dokumentinnhenting.LegeerklæringBestillingRequest
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.dokumentinnhenting.LegeerklæringPurringRequest
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.dokumentinnhenting.LegeerklæringStatusResponse
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.GetRequest
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.lookup.gateway.Factory
import java.net.URI

/**
 * Bestiller dokumenter fra dokumentinnhenting
 */
class DokumentinnhentingGatewayImpl : DokumentinnhentingGateway {
    private val syfoUri = requiredConfigForKey("integrasjon.dokumentinnhenting.url") + "/syfo"
    private val config = ClientConfig(scope = requiredConfigForKey("integrasjon.dokumentinnhenting.scope"))

    private val client = RestClient.withDefaultResponseHandler(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider,
    )

    companion object : Factory<DokumentinnhentingGateway>{
        override fun konstruer(): DokumentinnhentingGateway {
            return DokumentinnhentingGatewayImpl()
        }
    }

    override fun bestillLegeerklæring(request: LegeerklæringBestillingRequest): String {
        val request = PostRequest(
            body = request,
            additionalHeaders = listOf(
                Header("Nav-Consumer-Id", "aap-behandlingsflyt"),
                Header("Accept", "application/json")
            )
        )

        try {
            return requireNotNull(client.post(uri = URI.create("$syfoUri/dialogmeldingbestilling"), request))
        } catch (e : Exception) {
            throw RuntimeException("Feil ved bestilling av legeerklæring i aap-dokumentinnhenting: ${e.message}")
        }
    }

    override fun purrPåLegeerklæring(purringRequest: LegeerklæringPurringRequest): String {
        val request = PostRequest(
            body = purringRequest,
            additionalHeaders = listOf(
                Header("Nav-Consumer-Id", "aap-behandlingsflyt"),
                Header("Accept", "application/json")
            )
        )

        try {
            return requireNotNull(client.post(uri = URI.create("$syfoUri/purring"), request))
        } catch (e : Exception) {
            throw RuntimeException("Feil ved purring av legeerklæring i aap-dokumentinnhenting: ${e.message}")
        }
    }

    override fun legeerklæringStatus(saksnummer: String): List<LegeerklæringStatusResponse> {
        val request = GetRequest(
            additionalHeaders = listOf(
                Header("Nav-Consumer-Id", "aap-behandlingsflyt"),
                Header("Accept", "application/json")
            )
        )

        try {
            return requireNotNull(client.get(uri = URI.create("$syfoUri/status/$saksnummer"), request = request, mapper = { body, _ -> DefaultJsonMapper.fromJson(body) }))
        } catch (e: Exception) {
            throw RuntimeException("Feil ved innhentning av status til legeerklæring i aap-dokumentinnhenting: ${e.message}")
        }
    }

    override fun forhåndsvisBrev(request: BrevRequest): BrevResponse {
        val request = PostRequest(
            body = request,
            additionalHeaders = listOf(
                Header("Nav-Consumer-Id", "aap-behandlingsflyt"),
                Header("Accept", "application/json")
            )
        )

        try {
            return requireNotNull(client.post(uri = URI.create("$syfoUri/brevpreview"), request))
        } catch (e: Exception) {
            throw RuntimeException("Feil ved generering av forhåndsvisning brev i aap-dokumentinnhenting: ${e.message}")
        }
    }
}