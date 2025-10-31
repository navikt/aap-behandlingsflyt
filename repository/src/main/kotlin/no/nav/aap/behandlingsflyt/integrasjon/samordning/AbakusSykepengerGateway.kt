package no.nav.aap.behandlingsflyt.integrasjon.samordning

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.SykepengerGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.SykepengerResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering.gateway.UtbetaltePerioder
import no.nav.aap.behandlingsflyt.prometheus
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.gateway.Factory
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import org.slf4j.LoggerFactory
import java.net.URI
import java.time.LocalDate

class AbakusSykepengerGateway : SykepengerGateway {
    private val url = URI.create(requiredConfigForKey("integrasjon.sykepenger.url") + "/utbetalte-perioder-aap")
    private val config = ClientConfig(scope = requiredConfigForKey("integrasjon.sykepenger.scope"))

    private val secureLogger = LoggerFactory.getLogger("secureLog")

    companion object : Factory<SykepengerGateway> {
        override fun konstruer(): SykepengerGateway {
            return AbakusSykepengerGateway()
        }
    }

    private val client = RestClient.withDefaultResponseHandler(
        config = config, tokenProvider = ClientCredentialsTokenProvider, prometheus = prometheus
    )

    private fun query(request: SykepengerRequest): SykepengerResponse {
        val httpRequest = PostRequest(
            body = request, additionalHeaders = listOf(
                Header("Accept", "application/json")
            )
        )

        val response: SykepengerResponse? = client.post(uri = url, request = httpRequest)
        secureLogger.info("Sykepenger request: ${request}, response: $response")
        return requireNotNull(response)
    }

    override fun hentYtelseSykepenger(
        personidentifikatorer: Set<String>,
        fom: LocalDate,
        tom: LocalDate
    ): List<UtbetaltePerioder> {
        return query(SykepengerRequest(personidentifikatorer, fom, tom)).utbetaltePerioder
    }
}


/**
 * @param personidentifikatorer Er for en liste om vedkommende har hatt flere personidentifikatorerer. Ikke for å slå opp fler personer i samme oppslag. Da blir responsen bare krøll - Team SP
 */
private data class SykepengerRequest(
    val personidentifikatorer: Set<String>,
    val fom: LocalDate,
    val tom: LocalDate
)