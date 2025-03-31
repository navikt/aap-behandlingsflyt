package no.nav.aap.behandlingsflyt.tilgang

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
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
import no.nav.aap.tilgang.SakTilgangRequest
import no.nav.aap.tilgang.TilgangGateway.harTilgangTilBehandling
import no.nav.aap.tilgang.TilgangGateway.harTilgangTilSak
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

    override fun sjekkTilgangTilBehandling(behandlingsreferanse: UUID, avklaringsbehovKode: String, token: OidcToken): Boolean {
        return harTilgangTilBehandling(
            BehandlingTilgangRequest(
                behandlingsreferanse = behandlingsreferanse,
                avklaringsbehovKode = avklaringsbehovKode,
                operasjon = Operasjon.SAKSBEHANDLE
            ), token
        )
    }

    override fun sjekkTilgangTilSak(saksnummer: Saksnummer, token: OidcToken): Boolean {
        return harTilgangTilSak(
            SakTilgangRequest(
                saksnummer = saksnummer.toString(),
                operasjon = Operasjon.SAKSBEHANDLE
            ), token
        )
    }
}