package no.nav.aap.behandlingsflyt.integrasjon.dokarkiv

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.dokarkiv.DokarkivGateway
import no.nav.aap.behandlingsflyt.prometheus
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.error.ConflictHttpResponseException
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.komponenter.json.DefaultJsonMapper
import java.net.URI

object DokarkivGatewayImpl : DokarkivGateway {
    private val baseUrl = requiredConfigForKey("dokarkiv.url")

    private val httpClient = RestClient.withDefaultResponseHandler(
        ClientConfig(scope = requiredConfigForKey("dokarkiv.scope")),
        tokenProvider = ClientCredentialsTokenProvider,
        prometheus = prometheus
    )

    override fun oppdater(
        journalpost: DokarkivGateway.Journalpost,
        forsøkFerdigstill: Boolean
    ): DokarkivGateway.JournalpostResponse {
        return try {
            httpClient.post<_, DokarkivGateway.JournalpostResponse>(
                uri = URI("$baseUrl/rest/journalpostapi/v1/journalpost?forsoekFerdigstill=$forsøkFerdigstill"),
                request = PostRequest(
                    body = journalpost,
                    additionalHeaders = listOf(Header("accept", "application/json")),
                ),
            )!!
        } catch (e: ConflictHttpResponseException) {
            DefaultJsonMapper.fromJson<DokarkivGateway.JournalpostResponse>(e.body!!)
        }
    }
}