package no.nav.aap.behandlingsflyt.hendelse.oppgavestyring

import io.ktor.http.*
import no.nav.aap.behandlingsflyt.hendelse.avløp.BehandlingFlytStoppetHendelse
import no.nav.aap.httpclient.ClientConfig
import no.nav.aap.httpclient.RestClient
import no.nav.aap.httpclient.request.PostRequest
import no.nav.aap.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.requiredConfigForKey
import java.net.URI

object OppgavestyringGateway {
    private val url = URI.create(requiredConfigForKey("integrasjon.oppgavestyring.url"))
    private val config = ClientConfig(scope = requiredConfigForKey("integrasjon.oppgavestyring.scope"))

    private val client = RestClient(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider
    )

    fun varsleHendelse(hendelse: BehandlingFlytStoppetHendelse) {
        client.post<_, Unit>(url.resolve("/behandling"), PostRequest(body = hendelse))
    }

}