package no.nav.aap.behandlingsflyt.integrasjon.oppgave

import no.nav.aap.behandlingsflyt.hendelse.oppgavestyring.OppgavestyringGateway
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.BehandlingFlytStoppetHendelse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.TilbakekrevingHendelseKafkaMelding
import no.nav.aap.behandlingsflyt.prometheus
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import java.net.URI

object OppgavestyringGatewayImpl : OppgavestyringGateway {
    private val url = URI.create(requiredConfigForKey("integrasjon.oppgavestyring.url"))
    private val config = ClientConfig(scope = requiredConfigForKey("integrasjon.oppgavestyring.scope"))

    private val client = RestClient.withDefaultResponseHandler(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider,
        prometheus = prometheus
    )

    override fun varsleHendelse(hendelse: BehandlingFlytStoppetHendelse) {
        client.post<_, Unit>(url.resolve("/oppdater-oppgaver"), PostRequest(body = hendelse))
    }

    override fun varsleTilbakekrevingHendelse(hendelse: TilbakekrevingHendelseKafkaMelding) {
        client.post<_, Unit>(url.resolve("/oppdater-tilbakekreving-oppgaver"), PostRequest(body = hendelse))
    }

}
