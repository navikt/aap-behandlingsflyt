package no.nav.aap.behandlingsflyt.hendelse.oppgavestyring

import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import java.net.URI

object OppgavestyringGateway {
    private val url = URI.create(requiredConfigForKey("integrasjon.oppgavestyring.url"))
    private val config = ClientConfig(scope = requiredConfigForKey("integrasjon.oppgavestyring.scope"))

    private val client = RestClient.withDefaultResponseHandler(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider,
    )

    fun varsleHendelse(hendelse: BehandlingsFlytStoppetHendelseDTO) {
        client.post<_, Unit>(url.resolve("/behandling"), PostRequest(body = hendelse))
    }
}
