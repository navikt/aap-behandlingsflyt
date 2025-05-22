package no.nav.aap.behandlingsflyt.integrasjon.datadeling

import no.nav.aap.behandlingsflyt.datadeling.sam.SamGateway
import no.nav.aap.behandlingsflyt.datadeling.sam.SamordneVedtakRequest
import no.nav.aap.behandlingsflyt.datadeling.sam.SamordneVedtakRespons
import no.nav.aap.behandlingsflyt.prometheus
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.gateway.Factory
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.request.GetRequest
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import java.net.URI

class SamGatewayImpl : SamGateway {
    companion object : Factory<SamGateway> {
        override fun konstruer(): SamGateway {
            return SamGatewayImpl()
        }
    }

    private val restClient = RestClient.withDefaultResponseHandler(
        config = ClientConfig(scope = requiredConfigForKey("sam.scope")),
        tokenProvider = ClientCredentialsTokenProvider,
        prometheus = prometheus
    )

    val uri = URI.create(requiredConfigForKey("integrasjon.sam.url"))


    override fun varsleVedtak(request: SamordneVedtakRequest) {
        restClient.post<SamordneVedtakRequest, _>(
            uri = uri.resolve("/api/vedtak"),
            request = PostRequest(body = request),
            mapper = { samordneVedtakRespons, _ ->
                samordneVedtakRespons
            }
        )
    }

    override fun hentSamId(ident: Ident, sakId: String, vedtakId: String) {
        restClient.get(
            uri = uri.resolve("/api/vedtak?sakId=$sakId&vedtakId=$vedtakId&fagomrade=AAP"),
            request = GetRequest(
                additionalHeaders = listOf(Header("pid", ident.identifikator))
            ),
            mapper = { SamIdRespons, _ ->

            }
        )
    }
}