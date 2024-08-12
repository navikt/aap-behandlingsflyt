package no.nav.aap.tilgang

import no.nav.aap.httpclient.ClientConfig
import no.nav.aap.httpclient.RestClient
import no.nav.aap.httpclient.post
import no.nav.aap.httpclient.request.PostRequest
import no.nav.aap.httpclient.tokenprovider.OidcToken
import no.nav.aap.httpclient.tokenprovider.azurecc.OnBehalfOfTokenProvider
import no.nav.aap.requiredConfigForKey
import java.net.URI

object TilgangGateway {
    private val baseUrl = URI.create(requiredConfigForKey("integrasjon.tilgang.url"))
    private val config = ClientConfig(scope = requiredConfigForKey("integrasjon.tilgang.scope"))

    private val client = RestClient.withDefaultResponseHandler(
        config = config,
        tokenProvider = OnBehalfOfTokenProvider,
    )

    fun harTilgang(body: TilgangRequest, currentToken: OidcToken): Boolean {
        val respons = query(
            body,
            currentToken = currentToken
        )
        return respons.tilgang
    }

    private fun query(body: TilgangRequest, currentToken: OidcToken): TilgangResponse {
        val httpRequest = PostRequest(
            body = body,
            currentToken = currentToken
        )
        return requireNotNull(
            client.post<_, TilgangResponse>(
                uri = baseUrl.resolve("/tilgang"),
                request = httpRequest
            )
        )
    }
}

data class TilgangRequest(
    val saksnummer: String?,
    val behandlingsreferanse: String?,
    val avklaringsbehovKode: String?,
    val operasjon: Operasjon
)

enum class Operasjon {
    SE,
    SAKSBEHANDLE,
    DRIFTE,
    DELEGERE
}

data class TilgangResponse(val tilgang: Boolean)