package no.nav.aap.behandlingsflyt.behandling.underveis.sykepenger

import no.nav.aap.httpclient.ClientConfig
import no.nav.aap.httpclient.Header
import no.nav.aap.httpclient.RestClient
import no.nav.aap.httpclient.post
import no.nav.aap.httpclient.request.PostRequest
import no.nav.aap.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.requiredConfigForKey
import java.net.URI

class SykepengerGateway {
    private val url = URI.create(requiredConfigForKey("integrasjon.sykepenger.url") + "/utbetalte-perioder")
    val config = ClientConfig(scope = requiredConfigForKey("integrasjon.sykepenger.scope"))

    private val client = RestClient.withDefaultResponseHandler(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider,
    )

    private fun query(request: SykePengerRequest): SykePengerResponse {
        val httpRequest = PostRequest(
            body = request,
            additionalHeaders = listOf(
                Header("Accept", "application/json")
            )
        )
        return requireNotNull(client.post(uri = url, request = httpRequest))
    }

    fun hentYtelseSykepenger(request: SykePengerRequest): SykePengerResponse {
        try {
            return query(request)
        } catch (e : Exception) {
            throw RuntimeException("Feil ved henting av ytelser i sykepenger: ${e.message}")
        }
    }
}