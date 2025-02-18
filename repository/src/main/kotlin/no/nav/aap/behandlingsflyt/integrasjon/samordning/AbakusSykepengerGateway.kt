package no.nav.aap.behandlingsflyt.integrasjon.samordning

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.SykepengerGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.SykepengerRequest
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.SykepengerResponse
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.lookup.gateway.Factory
import java.net.URI

class AbakusSykepengerGateway : SykepengerGateway {
    private val url = URI.create(requiredConfigForKey("integrasjon.sykepenger.url") + "/utbetalte-perioder-aap")
    private val config = ClientConfig(scope = requiredConfigForKey("integrasjon.sykepenger.scope"))

    companion object : Factory<SykepengerGateway> {
        override fun konstruer(): SykepengerGateway {
            return AbakusSykepengerGateway()
        }
    }

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

    override fun hentYtelseSykepenger(request: SykepengerRequest): SykepengerResponse {
        return query(request)
    }
}