package no.nav.aap.behandlingsflyt.behandling.underveis.sykepenger

import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import java.net.URI

class SykepengerGateway {
    private val url = URI.create(requiredConfigForKey("integrasjon.sykepenger.url") + "/utbetalte-perioder")
    val config = ClientConfig(scope = requiredConfigForKey("integrasjon.sykepenger.scope"))

    private val client = RestClient.withDefaultResponseHandler(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider,
    )

    private fun query(request: SykepengerRequest): SykepengerResponse {
        val httpRequest = PostRequest(
            body = request,
            additionalHeaders = listOf(
                Header("Accept", "application/json")
            )
        )
        return requireNotNull(client.post(uri = url, request = httpRequest))
    }

    fun hentYtelseSykepenger(request: SykepengerRequest): SykepengerResponse {
        try {
            return query(request)
        } catch (e : Exception) {
            throw RuntimeException("Feil ved henting av ytelser i sykepenger: ${e.message}")
        }
    }
}