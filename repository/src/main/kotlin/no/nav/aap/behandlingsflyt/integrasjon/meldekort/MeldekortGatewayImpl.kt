package no.nav.aap.behandlingsflyt.integrasjon.meldekort

import no.nav.aap.behandlingsflyt.prometheus
import no.nav.aap.behandlingsflyt.prosessering.MeldekortGateway
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.gateway.Factory
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.meldekort.kontrakt.sak.MeldeperioderV0
import java.net.URI

class MeldekortGatewayImpl: MeldekortGateway {
    private val url = URI.create(requiredConfigForKey("integrasjon.meldekort.url"))

    private val client = RestClient.withDefaultResponseHandler(
        config = ClientConfig(scope = requiredConfigForKey("integrasjon.meldekort.scope")),
        tokenProvider = ClientCredentialsTokenProvider,
        prometheus = prometheus
    )

    private val oppdaterMeldeperiodeUrl = URI("$url/api/behandlingsflyt/sak/meldeperioder")

    override fun oppdaterMeldeperioder(meldeperioderV0: MeldeperioderV0) {
        client.post<MeldeperioderV0, Unit>(oppdaterMeldeperiodeUrl, PostRequest(meldeperioderV0))
    }

    companion object : Factory<MeldekortGateway> {
        override fun konstruer(): MeldekortGateway {
            return MeldekortGatewayImpl()
        }
    }
}