package no.nav.aap.behandlingsflyt.tilgang

import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.OnBehalfOfTokenProvider
import no.nav.aap.behandlingsflyt.prometheus
import no.nav.aap.tilgang.BehandlingTilgangRequest
import no.nav.aap.tilgang.Operasjon
import no.nav.aap.tilgang.TilgangResponse
import java.net.URI
import java.util.UUID


object TilgangGatewayImpl : TilgangGateway {
    private val baseUrl = URI.create(requiredConfigForKey("integrasjon.tilgang.url"))
    private val config = ClientConfig(scope = requiredConfigForKey("integrasjon.tilgang.scope"))

    private val client = RestClient.withDefaultResponseHandler(
        config = config,
        tokenProvider = OnBehalfOfTokenProvider,
        prometheus = prometheus
    )

    override fun sjekkTilgang(behandlingsreferanse: UUID, avklaringsbehovKode: String, token: OidcToken): Boolean {
        return harTilgangTilBehandling(
            BehandlingTilgangRequest(
                behandlingsreferanse = behandlingsreferanse,
                avklaringsbehovKode = avklaringsbehovKode,
                operasjon = Operasjon.SAKSBEHANDLE
            ), token
        )
    }

    private fun harTilgangTilBehandling(body: BehandlingTilgangRequest, currentToken: OidcToken): Boolean {
        val httpRequest = PostRequest(
            body = body,
            currentToken = currentToken
        )
        val respons = requireNotNull(
            client.post<_, TilgangResponse>(
                uri = baseUrl.resolve("/tilgang/behandling"),
                request = httpRequest
            )
        )
        return respons.tilgang
    }
}