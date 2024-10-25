package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.dokumentinnhenting

import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.GetRequest
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.komponenter.httpklient.json.DefaultJsonMapper
import java.net.URI

/**
 * Bestiller dokumenter fra dokumentinnhenting
 */
class DokumeninnhentingGateway {
    private val syfoUri = requiredConfigForKey("integrasjon.dokumentinnhenting.url" + "/syfo")
    val config = ClientConfig(scope = requiredConfigForKey("integrasjon.dokumentinnhenting.scope"))

    private val client = RestClient.withDefaultResponseHandler(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider,
    )

    fun bestillLegeerklæring(request: LegeerklæringBestillingRequest): LegeerklæringBestillingResponse {
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

    fun legeerklæringStatus(sakId: String): LegeerklæringStatusResponse {
        val request = GetRequest(
            additionalHeaders = listOf(
                Header("Nav-Consumer-Id", "aap-behandlingsflyt"),
                Header("Accept", "application/json")
            )
        )

        try {
            return requireNotNull(client.get(uri = URI.create("$syfoUri/status/{$sakId}"), request = request, mapper = { body, _ -> DefaultJsonMapper.fromJson(body) }))
        } catch (e: Exception) {
            throw RuntimeException("Feil ved innhentning av status til legeerklæring i aap-dokumentinnhenting: ${e.message}")
        }
    }
}