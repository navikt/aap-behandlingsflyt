package no.nav.aap.behandlingsflyt.hendelse.datadeling

import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.komponenter.type.Periode
import java.io.InputStream
import java.net.URI

class ApiInternGateway(restClient: RestClient<String>? = null) {
    private val restClient = restClient ?: RestClient.withDefaultResponseHandler(
        config = ClientConfig(scope = requiredConfigForKey("integrasjon.datadeling.scope")),
        tokenProvider = ClientCredentialsTokenProvider
    )

    private val uri = URI.create(requiredConfigForKey("integrasjon.datadeling.url"))

    fun sendPerioder(ident: String, perioder: List<Periode>) {
        restClient.post<_, Unit>(
            uri = uri.resolve("/api/insert/meldeperioder"),
            request = PostRequest(body = MeldekortPerioderDTO(ident, perioder)),
            mapper = { body, _ ->
                DefaultJsonMapper.fromJson(body as InputStream)
            })
    }

}