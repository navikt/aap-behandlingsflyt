package no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters

import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Saksnummer
import no.nav.aap.httpclient.ClientConfig
import no.nav.aap.httpclient.RestClient
import no.nav.aap.httpclient.post
import no.nav.aap.httpclient.request.PostRequest
import no.nav.aap.httpclient.tokenprovider.OidcToken
import no.nav.aap.httpclient.tokenprovider.azurecc.OnBehalfOfTokenProvider
import no.nav.aap.requiredConfigForKey
import no.nav.aap.saf.Journalpost
import no.nav.aap.saf.SafDokumentoversiktFagsakDataResponse
import no.nav.aap.saf.SafResponseHandler
import no.nav.aap.saf.Variantformat
import no.nav.aap.verdityper.dokument.DokumentInfoId
import no.nav.aap.verdityper.dokument.JournalpostId
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.net.URI
import java.net.http.HttpHeaders

val log = LoggerFactory.getLogger(SafListDokumentGateway::class.java)

object SafListDokumentGateway {
    private val graphqlUrl = URI.create(requiredConfigForKey("integrasjon.saf.url.graphql"))

    val config = ClientConfig(
        scope = requiredConfigForKey("integrasjon.saf.scope"),
    )

    private val client = RestClient(
        config = config,
        tokenProvider = OnBehalfOfTokenProvider,
        errorHandler = SafResponseHandler()
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
}

fun konstruerSafRestURL(
    baseUrl: URI,
    journalpostId: JournalpostId,
    dokumentInfoId: DokumentInfoId,
    variantFormat: String
): URI {
    return URI.create("$baseUrl/hentdokument/${journalpostId.identifikator}/${dokumentInfoId.dokumentInfoId}/${variantFormat}")
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
    val brevkode: String?,
    val tittel: String,
    val variantformat: Variantformat
)


data class SafDocumentResponse(val dokument: InputStream, val contentType: String, val filnavn: String)

fun String.asQuery() = this.replace("\n", "")

internal data class SafRequest(val query: String, val variables: Variables) {
    data class Variables(val fagsakId: String)
}

private const val fagsakId = "\$fagsakId"
// Skjema her: https://github.com/navikt/saf/blob/master/app/src/main/resources/schemas/saf.graphqls
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
