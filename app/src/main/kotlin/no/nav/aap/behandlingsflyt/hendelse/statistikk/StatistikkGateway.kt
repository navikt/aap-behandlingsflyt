package no.nav.aap.behandlingsflyt.hendelse.statistikk

import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.komponenter.httpklient.json.DefaultJsonMapper
import no.nav.aap.statistikk.api_kontrakt.AvsluttetBehandlingDTO
import no.nav.aap.statistikk.api_kontrakt.MottaStatistikkDTO
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.net.URI

// TODO: fjern senere?
private val SECURE_LOGGER = LoggerFactory.getLogger("secureLog")

class StatistikkGateway(restClient: RestClient<String>? = null) {
    // TODO: legg på auth mellom appene
    private val restClient = restClient ?: RestClient.withDefaultResponseHandler(
        config = ClientConfig(scope = requiredConfigForKey("integrasjon.statistikk.scope")),
        tokenProvider = ClientCredentialsTokenProvider
    )

    private val uri = URI.create(requiredConfigForKey("integrasjon.statistikk.url"))

    fun avgiStatistikk(hendelse: MottaStatistikkDTO) {
        SECURE_LOGGER.info("Avgir statistikk. Payload: $hendelse")
        restClient.post<_, Unit>(uri = uri.resolve("/motta"), request = PostRequest(body = hendelse), mapper = { body, _ ->
            DefaultJsonMapper.fromJson(body as InputStream)
        })
    }

    fun avsluttetBehandling(hendelse: AvsluttetBehandlingDTO) {
        SECURE_LOGGER.info("Avgir avsluttet behandling-statistikk. Payload: $hendelse")
        restClient.post<_, Unit>(uri = uri.resolve("/avsluttetBehandling"), request = PostRequest(body = hendelse), mapper = { body, _ ->
            DefaultJsonMapper.fromJson(body as InputStream)
        })
    }
}