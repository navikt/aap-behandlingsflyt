package no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters

import no.nav.aap.httpclient.ClientConfig
import no.nav.aap.httpclient.RestClient
import no.nav.aap.httpclient.error.InputStreamResponseHandler
import no.nav.aap.httpclient.request.GetRequest
import no.nav.aap.httpclient.tokenprovider.OidcToken
import no.nav.aap.httpclient.tokenprovider.azurecc.OnBehalfOfTokenProvider
import no.nav.aap.requiredConfigForKey
import no.nav.aap.verdityper.dokument.DokumentInfoId
import no.nav.aap.verdityper.dokument.JournalpostId
import java.io.InputStream
import java.net.URI

class SafHentDokumentGateway(private val restClient: RestClient<InputStream>) {
    private val restUrl = URI.create(requiredConfigForKey("integrasjon.saf.url.rest"))

    companion object {
        val config = ClientConfig(
            scope = requiredConfigForKey("integrasjon.saf.scope"),
        )

        fun withDefaultRestClient(): SafHentDokumentGateway {
            return SafHentDokumentGateway(
                RestClient(
                    config = config,
                    tokenProvider = OnBehalfOfTokenProvider,
                    responseHandler = InputStreamResponseHandler()
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
}