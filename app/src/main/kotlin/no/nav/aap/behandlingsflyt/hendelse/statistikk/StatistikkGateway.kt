package no.nav.aap.behandlingsflyt.hendelse.statistikk

import no.nav.aap.httpclient.ClientConfig
import no.nav.aap.httpclient.RestClient
import no.nav.aap.httpclient.post
import no.nav.aap.httpclient.request.PostRequest
import no.nav.aap.httpclient.tokenprovider.NoTokenTokenProvider
import no.nav.aap.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.requiredConfigForKey
import org.slf4j.LoggerFactory
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

    fun avgiStatistikk(hendelse: StatistikkHendelseDTO) {
        SECURE_LOGGER.info("Avgir statistikk. Payload: $hendelse")
        restClient.post<_, Unit>(uri = uri.resolve("/motta"), request = PostRequest(body = hendelse))
    }

    fun avsluttetBehandling(hendelse: AvsluttetBehandlingDTO) {
        SECURE_LOGGER.info("Avgir avsluttet behandling-statistikk. Payload: $hendelse")
        restClient.post<_, Unit>(uri = uri.resolve("/avsluttetBehandling"), request = PostRequest(body = hendelse))
    }
}