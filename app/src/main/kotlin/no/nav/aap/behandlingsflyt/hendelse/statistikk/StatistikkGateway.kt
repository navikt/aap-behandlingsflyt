package no.nav.aap.behandlingsflyt.hendelse.statistikk

import no.nav.aap.httpclient.ClientConfig
import no.nav.aap.httpclient.RestClient
import no.nav.aap.httpclient.post
import no.nav.aap.httpclient.request.PostRequest
import no.nav.aap.httpclient.tokenprovider.NoTokenTokenProvider
import no.nav.aap.requiredConfigForKey
import java.net.URI

class StatistikkGateway {
    // TODO: legg på auth mellom appene
    private val restClient =
        RestClient.withDefaultResponseHandler(config = ClientConfig(), tokenProvider = NoTokenTokenProvider())

    private val uri = URI.create(requiredConfigForKey("integrasjon.statistikk.url"))

    fun avgiStatistikk(hendelse: StatistikkHendelseDTO) {
        restClient.post<_, Unit>(uri = uri.resolve("/motta"), request = PostRequest(body = hendelse))
    }
}