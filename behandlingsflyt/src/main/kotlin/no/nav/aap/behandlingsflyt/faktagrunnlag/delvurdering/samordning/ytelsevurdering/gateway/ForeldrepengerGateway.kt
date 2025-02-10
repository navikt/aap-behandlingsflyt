package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway

import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import java.net.URI

/**
 * Henter alle ytelser i fpabakus
 *
 * TODO: har disse en offisiel Swagger noe steD?
 */
class ForeldrepengerGateway {
    private val url = URI.create(requiredConfigForKey("integrasjon.foreldrepenger.url") + "/hent-ytelse-vedtak")
    val config = ClientConfig(scope = requiredConfigForKey("integrasjon.foreldrepenger.scope"))

    private val client = RestClient.withDefaultResponseHandler(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider,
    )

    private fun query(request: ForeldrepengerRequest): ForeldrepengerResponse {
        val httpRequest = PostRequest(
            body = request,
            additionalHeaders = listOf(
                Header("Accept", "application/json")
            )
        )
        val response: List<Ytelse> = requireNotNull(client.post(uri = url, request = httpRequest))
        return ForeldrepengerResponse(response)
    }

    fun hentVedtakYtelseForPerson(request: ForeldrepengerRequest): ForeldrepengerResponse {
        val result = query(request)
        return result

    }
}