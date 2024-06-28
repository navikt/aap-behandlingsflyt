package no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters

import no.nav.aap.httpclient.ClientConfig
import no.nav.aap.httpclient.RestClient
import no.nav.aap.httpclient.post
import no.nav.aap.httpclient.request.PostRequest
import no.nav.aap.httpclient.tokenprovider.azurecc.OnBehalfOfTokenProvider
import no.nav.aap.requiredConfigForKey
import no.nav.aap.verdityper.sakogbehandling.Ident
import java.net.URI

object TilgangGateway {
    private val baseUrl = URI.create(requiredConfigForKey("integrasjon.tilgang.url"))
    private val config = ClientConfig(scope = requiredConfigForKey("integrasjon.tilgang.scope"))

    private val client = RestClient.withDefaultResponseHandler(
        config = config,
        tokenProvider = OnBehalfOfTokenProvider,
    )

    fun kanLeseSak(identer: List<Ident>): Boolean {
        val respons = query(
            "lese",
            TilgangRequest(identer.map { it.identifikator })
        )
        return respons.tilgang
    }

    private fun query(endepunkt: String, request: TilgangRequest): TilgangResponse {
        val httpRequest = PostRequest(
            body = request,
        )
        return requireNotNull(
            client.post<_, TilgangResponse>(
                uri = baseUrl.resolve("/tilgang/$endepunkt"),
                request = httpRequest
            )
        )
    }
}

data class TilgangRequest(
    val identer: List<String>
)

data class TilgangResponse(val tilgang: Boolean)