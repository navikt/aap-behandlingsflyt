package no.nav.aap.behandlingsflyt.faktagrunnlag.register.aaregisteret

import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import java.net.URI

class AARegisterGateway {
    private val url = URI.create(requiredConfigForKey("integrasjon.aareg.url")+"/api/v2/arbeidstaker/arbeidsforholdoversikt")
    private val config = ClientConfig(scope = requiredConfigForKey("integrasjon.aareg.scope"))

    private val client = RestClient.withDefaultResponseHandler(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider,
    )

    private fun query(request: ArbeidsforholdRequest): ArbeidsforholdoversiktResponse {
        val httpRequest = PostRequest(
            body = request,
            additionalHeaders = listOf(
                Header("Accept", "application/json")
            )
        )
        return requireNotNull(client.post(uri = url, request = httpRequest))
    }

    fun hentAARegisterData(request: ArbeidsforholdRequest): ArbeidsforholdoversiktResponse {
        try {
            return query(request)
        } catch (e : Exception) {
            throw RuntimeException("Feil ved henting av data i AAREG: ${e.message}")
        }
    }
}