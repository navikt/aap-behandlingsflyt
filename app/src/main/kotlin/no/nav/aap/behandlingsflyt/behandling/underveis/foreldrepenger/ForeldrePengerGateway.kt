package no.nav.aap.behandlingsflyt.behandling.underveis.foreldrepenger

import no.nav.aap.httpclient.ClientConfig
import no.nav.aap.httpclient.Header
import no.nav.aap.httpclient.RestClient
import no.nav.aap.httpclient.post
import no.nav.aap.httpclient.request.PostRequest
import no.nav.aap.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.requiredConfigForKey
import java.net.URI

/**
 * Henter foreldrepenger og svangerskapspenger for gitt periode
 */
class ForeldrePengerGateway {
    private val url = URI.create(requiredConfigForKey("integrasjon.foreldrepenger.url") + "/hent-ytelse-vedtak")
    val config = ClientConfig(scope = requiredConfigForKey("integrasjon.foreldrepenger.scope"))

    private val client = RestClient.withDefaultResponseHandler(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider,
    )

    private fun query(request: ForeldrePengerRequest): List<Ytelse> {
        val httpRequest = PostRequest(
            body = request,
            additionalHeaders = listOf(
                Header("Accept", "application/json")
            )
        )
        return requireNotNull(client.post(uri = url, request = httpRequest))
    }

    fun hentVedtakYtelseForPerson(request: ForeldrePengerRequest): ForeldrePengerResponse {
        try {
            val result = query(request)
            return ForeldrePengerResponse(result)
        } catch (e : Exception) {
            throw RuntimeException("Feil ved henting av ytelser i foreldrepenger: ${e.message}")
        }
    }
}