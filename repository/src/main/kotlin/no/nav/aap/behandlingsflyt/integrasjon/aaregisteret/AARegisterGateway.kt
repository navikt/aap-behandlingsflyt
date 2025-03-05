package no.nav.aap.behandlingsflyt.integrasjon.aaregisteret

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aaregisteret.ArbeidsforholdGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aaregisteret.ArbeidsforholdRequest
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.aaregisteret.ArbeidsforholdoversiktResponse
import no.nav.aap.behandlingsflyt.prometheus
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.error.IkkeFunnetException
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.lookup.gateway.Factory
import java.net.URI

class AARegisterGateway : ArbeidsforholdGateway {
    private val url =
        URI.create(requiredConfigForKey("integrasjon.aareg.url") + "/api/v2/arbeidstaker/arbeidsforholdoversikt")
    private val config = ClientConfig(scope = requiredConfigForKey("integrasjon.aareg.scope"))

    companion object : Factory<ArbeidsforholdGateway> {
        override fun konstruer(): ArbeidsforholdGateway {
            return AARegisterGateway()
        }
    }

    private val client = RestClient.withDefaultResponseHandler(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider,
        prometheus = prometheus
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

    override fun hentAARegisterData(request: ArbeidsforholdRequest): ArbeidsforholdoversiktResponse {
        return try {
            query(request)
        } catch (e: IkkeFunnetException) {
            // Fant ikke ident i AAreg, de returnerer 404
            ArbeidsforholdoversiktResponse()
        } catch (e: Exception) {
            throw RuntimeException("Feil ved henting av data i AAreg: ${e.message}", e)
        }
    }
}