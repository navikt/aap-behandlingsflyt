package no.nav.aap.behandlingsflyt.hendelse.statistikk

import no.nav.aap.behandlingsflyt.kontrakt.statistikk.StoppetBehandling
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.komponenter.httpklient.json.DefaultJsonMapper
import java.io.InputStream
import java.net.URI

class StatistikkGateway(restClient: RestClient<String>? = null) {

    private val restClient = restClient ?: RestClient.withDefaultResponseHandler(
        config = ClientConfig(scope = requiredConfigForKey("integrasjon.statistikk.scope")),
        tokenProvider = ClientCredentialsTokenProvider
    )

    private val uri = URI.create(requiredConfigForKey("integrasjon.statistikk.url"))

    fun avgiStatistikk(hendelse: StoppetBehandling) {
        restClient.post<_, Unit>(
            uri = uri.resolve("/stoppetBehandling"),
            request = PostRequest(body = hendelse),
            mapper = { body, _ ->
                DefaultJsonMapper.fromJson(body as InputStream)
            })
    }
}