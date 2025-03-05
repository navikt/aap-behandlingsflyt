package no.nav.aap.behandlingsflyt.integrasjon.samordning

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.ForeldrepengerGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.ForeldrepengerRequest
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.ForeldrepengerResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.Ytelse
import no.nav.aap.behandlingsflyt.prometheus
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.lookup.gateway.Factory
import java.net.URI

/**
 * Henter alle ytelser i fpabakus
 */
class AbakusForeldrepengerGateway : ForeldrepengerGateway {
    private val url = URI.create(requiredConfigForKey("integrasjon.foreldrepenger.url") + "/hent-ytelse-vedtak")
    private val config = ClientConfig(scope = requiredConfigForKey("integrasjon.foreldrepenger.scope"))

    companion object : Factory<ForeldrepengerGateway> {
        override fun konstruer(): ForeldrepengerGateway {
            return AbakusForeldrepengerGateway()
        }
    }

    private val client = RestClient.withDefaultResponseHandler(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider,
        prometheus = prometheus
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

    override fun hentVedtakYtelseForPerson(request: ForeldrepengerRequest): ForeldrepengerResponse {
        val result = query(request)
        return result
    }
}