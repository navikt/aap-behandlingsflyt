package no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters

import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Saksnummer
import no.nav.aap.httpclient.ClientConfig
import no.nav.aap.httpclient.RestClient
import no.nav.aap.httpclient.request.GetRequest
import no.nav.aap.httpclient.request.PostRequest
import no.nav.aap.httpclient.tokenprovider.OidcToken
import no.nav.aap.httpclient.tokenprovider.azurecc.OnBehalfOfTokenProvider
import no.nav.aap.requiredConfigForKey
import no.nav.aap.saf.Journalpost
import no.nav.aap.verdityper.dokument.DokumentInfoId
import no.nav.aap.verdityper.dokument.JournalpostId
import no.nav.aap.saf.SafDokumentoversiktFagsakDataResponse
import no.nav.aap.saf.SafResponseHandler
import no.nav.aap.saf.Variantformat
import java.net.URI
import java.net.http.HttpHeaders
import java.util.*

object SafGateway {
    private val graphqlUrl = URI.create(requiredConfigForKey("integrasjon.saf.url.graphql"))
    private val restUrl = URI.create(requiredConfigForKey("integrasjon.saf.url.rest"))

    val config = ClientConfig(
        scope = requiredConfigForKey("integrasjon.saf.scope"),
    )

    private val client = RestClient(
        config = config,
        tokenProvider = OnBehalfOfTokenProvider,
        errorHandler = SafResponseHandler(config = config)
    )

    private fun query(request: SafRequest, currentToken: OidcToken): SafDokumentoversiktFagsakDataResponse {
        val httpRequest = PostRequest(body = request, currentToken = currentToken)
        return requireNotNull(client.post(uri = graphqlUrl, request = httpRequest))
    }

    fun hentDokumenterForSak(saksnummer: Saksnummer, currentToken: OidcToken): List<Dokument> {
        val request = SafRequest(dokumentOversiktQuery.asQuery(), SafRequest.Variables(saksnummer.toString()))
        val response = query(request, currentToken)

        val dokumentoversiktFagsak = response.data?.dokumentoversiktFagsak ?: return emptyList()

        return dokumentoversiktFagsak.journalposter.tilArkivDokumenter()
    }

    fun hentDokument(
        journalpostId: JournalpostId,
        dokumentInfoId: DokumentInfoId,
        currentToken: OidcToken
    ): SafDocumentResponse {
        // Se https://confluence.adeo.no/display/BOA/Enum%3A+Variantformat
        // for gyldige verdier
        val variantFormat = "ARKIV"
        val respons = client.get(
            uri = restUrl.resolve("/${journalpostId}/${dokumentInfoId}/${variantFormat}"),
            request = GetRequest(currentToken = currentToken),
            mapper = { body, headers ->
                val contentType = headers.map()["Content-Type"]?.firstOrNull()
                val filnavn: String? = extractFileNameFromHeaders(headers)

                if (contentType == null || filnavn == null) {
                    throw IllegalStateException("Respons inneholdt ikke korrekte headere: $headers")
                }

                // TODO muligens les body som stream med annen responsebodyhandler
                val decodedResponse = Base64.getDecoder().decode(body)
                SafDocumentResponse(dokument = decodedResponse, contentType = contentType, filnavn = filnavn)
            }
        )

        return respons!!
    }
}

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

fun List<Journalpost>.tilArkivDokumenter(): List<Dokument> {
    return this.flatMap { journalpost ->
        journalpost.dokumenter.flatMap { dok ->
            dok.dokumentvarianter
                .filter { it.variantformat === Variantformat.ARKIV }
                .map {
                    Dokument(
                        journalpostId = journalpost.journalpostId,
                        dokumentInfoId = dok.dokumentInfoId,
                        tittel = dok.tittel,
                        brevkode = dok.brevkode,
                        variantformat = it.variantformat
                    )
                }
        }
    }
}

data class Dokument(
    val dokumentInfoId: String,
    val journalpostId: String,
    val brevkode: String,
    val tittel: String,
    val variantformat: Variantformat
)


data class SafDocumentResponse(val dokument: ByteArray, val contentType: String, val filnavn: String) {
    // equals, hashCode må implementeres fordi dokument ikke er en immutable klasse
    // Implementasjon auto-generert av IntelliJ
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SafDocumentResponse

        if (!dokument.contentEquals(other.dokument)) return false
        if (contentType != other.contentType) return false
        if (filnavn != other.filnavn) return false

        return true
    }

    override fun hashCode(): Int {
        var result = dokument.contentHashCode()
        result = 31 * result + contentType.hashCode()
        result = 31 * result + filnavn.hashCode()
        return result
    }
}

enum class DokumentFormat {
    PDF, PNG
}

fun String.asQuery() = this.replace("\n", "")

internal data class SafRequest(val query: String, val variables: Variables) {
    data class Variables(val fagsakId: String)
}

private const val fagsakId = "\$fagsakId"
private val dokumentOversiktQuery = """
query ($fagsakId: String!)
{
  dokumentoversiktFagsak(
    fagsak: { fagsakId: $fagsakId, fagsaksystem: "KELVIN" }
   fraDato: null
   foerste: 100
    tema: []
    journalposttyper: []
    journalstatuser: []
  ) {
    journalposter {
      journalpostId
      behandlingstema
      antallRetur
      kanal
      innsynsregelBeskrivelse
      behandlingstema
      dokumenter {
        dokumentInfoId
        tittel
        brevkode
        dokumentstatus
        datoFerdigstilt
        originalJournalpostId
        skjerming
        logiskeVedlegg {
          logiskVedleggId
          tittel
        }
        dokumentvarianter {
          variantformat
          saksbehandlerHarTilgang
          skjerming
        }
      } 
    }
    sideInfo {
      sluttpeker
      finnesNesteSide
      antall
      totaltAntall
    }
  }
}
""".trimIndent()