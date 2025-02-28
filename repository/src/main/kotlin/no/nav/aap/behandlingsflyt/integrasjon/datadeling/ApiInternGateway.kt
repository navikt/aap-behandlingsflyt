package no.nav.aap.behandlingsflyt.integrasjon.datadeling

import no.nav.aap.behandlingsflyt.datadeling.SakStatus
import no.nav.aap.behandlingsflyt.datadeling.SakStatusDTO
import no.nav.aap.behandlingsflyt.hendelse.datadeling.ApiInternGateway
import no.nav.aap.behandlingsflyt.hendelse.datadeling.MeldekortPerioderDTO
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.gateway.Factory
import java.net.URI

class ApiInternGatewayImpl(restClient: RestClient<String>? = null) : ApiInternGateway {
    companion object : Factory<ApiInternGateway> {
        override fun konstruer(): ApiInternGateway {
            return ApiInternGatewayImpl()
        }
    }

    private val restClient = restClient ?: RestClient.withDefaultResponseHandler(
        config = ClientConfig(scope = requiredConfigForKey("integrasjon.datadeling.scope")),
        tokenProvider = ClientCredentialsTokenProvider
    )

    private val uri = URI.create(requiredConfigForKey("integrasjon.datadeling.url"))

    override fun sendPerioder(ident: String, perioder: List<Periode>) {
        restClient.post<_, Unit>(
            uri = uri.resolve("/api/insert/meldeperioder"),
            request = PostRequest(body = MeldekortPerioderDTO(ident, perioder)),
            mapper = { _, _ ->
                Unit
            })
    }

    override fun sendSakStatus(ident: String, sakStatus: SakStatus) {
        restClient.post<_, Unit>(
            uri = uri.resolve("/api/insert/sakStatus"),
            request = PostRequest(body = SakStatusDTO(ident, sakStatus)),
            mapper = { _, _ ->
                Unit
            })
    }
}