package no.nav.aap.behandlingsflyt.integrasjon.statistikk

import no.nav.aap.behandlingsflyt.hendelse.statistikk.StatistikkGateway
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.StoppetBehandling
import no.nav.aap.behandlingsflyt.prometheus
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.gateway.Factory
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.komponenter.json.DefaultJsonMapper
import java.io.InputStream
import java.net.URI

class StatistikkGatewayImpl : StatistikkGateway {
    companion object : Factory<StatistikkGateway> {
        override fun konstruer(): StatistikkGateway {
            return StatistikkGatewayImpl()
        }
    }

    private val restClient = RestClient.withDefaultResponseHandler(
        config = ClientConfig(scope = requiredConfigForKey("integrasjon.statistikk.scope")),
        tokenProvider = ClientCredentialsTokenProvider,
        prometheus = prometheus
    )

    private val uri = URI.create(requiredConfigForKey("integrasjon.statistikk.url"))

    override fun avgiStatistikk(hendelse: StoppetBehandling) {
        restClient.post<_, Unit>(
            uri = uri.resolve("/stoppetBehandling"),
            request = PostRequest(body = hendelse),
            mapper = { body, _ ->
                DefaultJsonMapper.fromJson(body)
            })
    }

    override fun resendBehandling(hendelse: StoppetBehandling) {
        restClient.post<_, Unit>(
            uri = uri.resolve("/oppdatertBehandling"),
            request = PostRequest(body = hendelse),
            mapper = { body, _ ->
                DefaultJsonMapper.fromJson(body)
            })
    }
}