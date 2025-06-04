package no.nav.aap.behandlingsflyt.integrasjon.arbeidsforhold

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.ereg.EnhetsregisteretGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.ereg.adapter.EnhetsregisterOrganisasjonRequest
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.ereg.adapter.EnhetsregisterOrganisasjonResponse
import no.nav.aap.behandlingsflyt.prometheus
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.gateway.Factory
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.error.IkkeFunnetException
import no.nav.aap.komponenter.httpklient.httpclient.get
import no.nav.aap.komponenter.httpklient.httpclient.request.GetRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import org.slf4j.LoggerFactory
import java.net.URI

class EREGGateway : EnhetsregisteretGateway {
    private val url = URI.create(requiredConfigForKey("integrasjon.ereg.url") + "/api/v2/organisasjon")
    private val config = ClientConfig(scope = requiredConfigForKey("integrasjon.ereg.scope"))
    private val log = LoggerFactory.getLogger(javaClass)

    companion object : Factory<EnhetsregisteretGateway> {
        override fun konstruer(): EnhetsregisteretGateway {
            return EREGGateway()
        }
    }

    private val client = RestClient.withDefaultResponseHandler(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider,
        prometheus = prometheus
    )

    private fun query(request: EnhetsregisterOrganisasjonRequest): EnhetsregisterOrganisasjonResponse {
        val httpRequest = GetRequest(
            additionalHeaders = listOf(
                Header("Accept", "application/json")
            )
        )
        val response: EnhetsregisterOrganisasjonResponse = requireNotNull(
            client.get(
                uri = URI.create("$url/${request.organisasjonsnummer}"),
                request = httpRequest
            )
        )
        return response
    }

    override fun hentEREGData(request: EnhetsregisterOrganisasjonRequest): EnhetsregisterOrganisasjonResponse? {
        return try {
            query(request)
        } catch (e: IkkeFunnetException) {
            log.warn("Fant ikke hente organisasjonsnavn. Fortsetter uten verdi.", e)
            null
        } catch (e: Exception) {
            log.warn("Feil ved henting av data i EREG. Fortsetter uten", e)
            return null
        }
    }
}