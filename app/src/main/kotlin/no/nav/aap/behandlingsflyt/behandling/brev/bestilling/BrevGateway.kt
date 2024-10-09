package no.nav.aap.behandlingsflyt.behandling.brev.bestilling

import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.brev.kontrakt.BestillBrevRequest
import no.nav.aap.brev.kontrakt.BestillBrevResponse
import no.nav.aap.brev.kontrakt.Brevtype
import no.nav.aap.brev.kontrakt.Språk
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.komponenter.httpklient.json.DefaultJsonMapper
import java.net.URI
import java.util.UUID

class BrevGateway : BrevbestillingGateway {

    private val baseUri = URI.create(requiredConfigForKey("integrasjon.brev.url"))
    val config = ClientConfig(
        scope = requiredConfigForKey("integrasjon.brev.scope"),
    )

    private val client = RestClient.withDefaultResponseHandler(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider,
    )

    override fun bestillBrev(
        behandlingReferanse: BehandlingReferanse,
        typeBrev: TypeBrev,
    ): UUID {
        // TODO språk
        val request = BestillBrevRequest(behandlingReferanse.referanse, mapTypeBrev(typeBrev), Språk.NB)

        val httpRequest = PostRequest<BestillBrevRequest>(
            body = request,
            additionalHeaders = listOf(
                Header("Accept", "application/json")
            )
        )

        val url = baseUri.resolve("/api/bestill")

        val response: BestillBrevResponse = requireNotNull(
            client.post(
                uri = url,
                request = httpRequest,
                mapper = { body, _ ->
                    DefaultJsonMapper.fromJson(body)
                })
        )

        return response.referanse
    }

    private fun mapTypeBrev(typeBrev: TypeBrev): Brevtype = when (typeBrev) {
        TypeBrev.VEDTAK_AVSLAG -> Brevtype.AVSLAG
        TypeBrev.VEDTAK_INNVILGELSE -> Brevtype.INNVILGELSE
    }
}
