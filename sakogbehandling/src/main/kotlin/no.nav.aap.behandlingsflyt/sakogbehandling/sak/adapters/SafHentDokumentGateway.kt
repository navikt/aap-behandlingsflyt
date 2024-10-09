package no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters

import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.request.GetRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.OnBehalfOfTokenProvider
import no.nav.aap.verdityper.dokument.DokumentInfoId
import no.nav.aap.verdityper.dokument.JournalpostId
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.net.URI
import java.net.http.HttpHeaders

private val log = LoggerFactory.getLogger(SafHentDokumentGateway::class.java)

class SafHentDokumentGateway(private val restClient: RestClient<InputStream>) {
    private val restUrl = URI.create(requiredConfigForKey("integrasjon.saf.url.rest"))

    companion object {
        val config = ClientConfig(
            scope = requiredConfigForKey("integrasjon.saf.scope"),
        )

        fun extractFileNameFromHeaders(headers: HttpHeaders): String? {
            val value = headers.map()["Content-Disposition"]?.firstOrNull()
            if (value.isNullOrBlank()) {
                return null
            }
            val regex =
                Regex("filename=([^;]+)")

            val matchResult = regex.find(value)
            return matchResult?.groupValues?.get(1)
        }

        fun withDefaultRestClient(): SafHentDokumentGateway {
            return SafHentDokumentGateway(
                RestClient.withDefaultResponseHandler(
                    config = config,
                    tokenProvider = OnBehalfOfTokenProvider
                )
            )
        }
    }

    fun hentDokument(
        journalpostId: JournalpostId,
        dokumentInfoId: DokumentInfoId,
        currentToken: OidcToken
    ): SafDocumentResponse {
        // Se https://confluence.adeo.no/display/BOA/Enum%3A+Variantformat
        // for gyldige verdier
        val variantFormat = "ARKIV"

        val safURI = konstruerSafRestURL(restUrl, journalpostId, dokumentInfoId, variantFormat)
        log.info("Kaller SAF med URL: ${safURI}.")
        val respons = restClient.get(
            uri = safURI,
            request = GetRequest(currentToken = currentToken),
            mapper = { body, headers ->
                val contentType = headers.map()["Content-Type"]?.firstOrNull()
                val filnavn: String? = extractFileNameFromHeaders(headers)

                if (contentType == null || filnavn == null) {
                    throw IllegalStateException("Respons inneholdt ikke korrekte headere: $headers")
                }
                SafDocumentResponse(dokument = body, contentType = contentType, filnavn = filnavn)
            }
        )

        return respons!!
    }

    private fun konstruerSafRestURL(
        baseUrl: URI,
        journalpostId: JournalpostId,
        dokumentInfoId: DokumentInfoId,
        variantFormat: String
    ): URI {
        return URI.create("$baseUrl/hentdokument/${journalpostId.identifikator}/${dokumentInfoId.dokumentInfoId}/${variantFormat}")
    }
}

data class SafDocumentResponse(val dokument: InputStream, val contentType: String, val filnavn: String)