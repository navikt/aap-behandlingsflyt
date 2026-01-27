package no.nav.aap.behandlingsflyt.integrasjon.samordning

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.gateway.DagpengerGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.gateway.DagpengerPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.gateway.DagpengerResponse
import no.nav.aap.behandlingsflyt.prometheus
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.gateway.Factory
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import java.net.URI

class DagpengerGatewayImpl: DagpengerGateway {
    private val url = URI.create(requiredConfigForKey("integrasjon.dagpenger.url") + "/dagpenger/datadeling/v1/perioder")
    private val config = ClientConfig(scope = requiredConfigForKey("integrasjon.dagpenger.scope"))

    companion object : Factory<DagpengerGateway> {
        override fun konstruer(): DagpengerGateway {
            return DagpengerGatewayImpl()
        }
    }

    private val client = RestClient.withDefaultResponseHandler(
        config = config, tokenProvider = ClientCredentialsTokenProvider, prometheus = prometheus
    )

    private fun query(request: DagpengerRequest): DagpengerResponse {
        val httpRequest = PostRequest(
            body = request, additionalHeaders = listOf(
                Header("Accept", "application/json")
            )
        )

        val response: DagpengerResponse? = client.post(uri = url, request = httpRequest)
        return requireNotNull(response)
    }

    override fun hentYtelseDagpenger(
        personidentifikatorer: String,
        fom: String,
        tom: String
    ): List<DagpengerPeriode> {
        return query(DagpengerRequest(
            personIdent = personidentifikatorer,
            fraOgMedDato = fom,
            tilOgMedDato = tom
        )).perioder
    }
}

private data class DagpengerRequest(
    val personIdent: String,
    val fraOgMedDato: String,
    val tilOgMedDato: String
)