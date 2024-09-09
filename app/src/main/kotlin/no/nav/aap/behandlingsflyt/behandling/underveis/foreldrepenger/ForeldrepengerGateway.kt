package no.nav.aap.behandlingsflyt.behandling.underveis.foreldrepenger

import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import org.slf4j.LoggerFactory
import java.net.URI

/**
 * Henter foreldrepenger og svangerskapspenger for gitt periode
 */
class ForeldrepengerGateway {
    private val url = URI.create(requiredConfigForKey("integrasjon.foreldrepenger.url") + "/hent-ytelse-vedtak")
    val config = ClientConfig(scope = requiredConfigForKey("integrasjon.foreldrepenger.scope"))

    private val log = LoggerFactory.getLogger(ForeldrepengerGateway::class.java)
    private val client = RestClient.withDefaultResponseHandler(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider,
    )

    private fun query(request: ForeldrepengerRequest): List<Ytelse> {
        val httpRequest = PostRequest(
            body = request,
            additionalHeaders = listOf(
                Header("Accept", "application/json")
            )
        )
        val resp = requireNotNull(client.post(uri = url, request = httpRequest))
        log.info("Response from fp: $resp")
        return resp as List<Ytelse>
    }

    fun hentVedtakYtelseForPerson(request: ForeldrepengerRequest): ForeldrepengerResponse {
        try {
            val result = query(request)
            return ForeldrepengerResponse(result)
        } catch (e : Exception) {
            throw RuntimeException("Feil ved henting av ytelser i foreldrepenger: ${e.message}")
        }
    }
}