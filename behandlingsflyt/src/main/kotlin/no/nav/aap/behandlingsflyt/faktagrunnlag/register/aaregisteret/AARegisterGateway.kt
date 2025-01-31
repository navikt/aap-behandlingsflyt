package no.nav.aap.behandlingsflyt.faktagrunnlag.register.aaregisteret

import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.error.IkkeFunnetException
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import org.slf4j.LoggerFactory
import java.net.URI

class AARegisterGateway {
    private val url = URI.create(requiredConfigForKey("integrasjon.aareg.url")+"/api/v2/arbeidstaker/arbeidsforholdoversikt")
    private val config = ClientConfig(scope = requiredConfigForKey("integrasjon.aareg.scope"))
    val logger = LoggerFactory.getLogger(AARegisterGateway::class.java)

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
        return try {
            query(request)
        } catch (e: IkkeFunnetException) {
            // Fant ikke ident i AAreg, de returnerer 404
            ArbeidsforholdoversiktResponse()
        } catch (e: Exception) {
            logger.error("Feil ved henting av data i AAreg: ${e.message}, stack{$e}")
            ArbeidsforholdoversiktResponse()
            //throw RuntimeException("Feil ved henting av data i AAreg: ${e.message}", e)
        }
    }
}