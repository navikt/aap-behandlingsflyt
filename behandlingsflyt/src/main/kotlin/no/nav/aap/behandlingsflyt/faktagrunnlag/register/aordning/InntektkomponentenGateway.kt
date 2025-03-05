package no.nav.aap.behandlingsflyt.faktagrunnlag.register.aordning

import no.nav.aap.behandlingsflyt.prometheus
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import java.net.URI
import java.time.YearMonth

class InntektkomponentenGateway  {
    private val url = URI.create(requiredConfigForKey("integrasjon.inntektskomponenten.url") +"/hentinntektliste")
    private val config = ClientConfig(scope = requiredConfigForKey("integrasjon.inntektskomponenten.scope"))

    private val client = RestClient.withDefaultResponseHandler(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider,
        prometheus = prometheus
    )

    private fun query(request: InntektskomponentRequest): InntektskomponentResponse {
        val httpRequest = PostRequest(
            body = request,
            additionalHeaders = listOf(
                Header("Accept", "application/json")
            )
        )
        return requireNotNull(client.post(uri = url, request = httpRequest))
    }

    fun hentAInntekt(fnr: String, fom: YearMonth, tom: YearMonth): InntektskomponentResponse {
        val request = InntektskomponentRequest(
            maanedFom = fom,
            maanedTom = tom,
            ident = Ident(
                fnr
            )
        )

        try {
            return query(request)
        } catch (e : Exception) {
            throw RuntimeException("Feil ved henting av data i Inntektskomponenten: ${e.message}, $e")
        }
    }
}