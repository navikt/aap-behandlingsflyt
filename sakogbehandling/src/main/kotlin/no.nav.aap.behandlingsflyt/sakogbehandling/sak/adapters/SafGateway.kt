package no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters

import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Saksnummer
import no.nav.aap.httpclient.ClientConfig
import no.nav.aap.httpclient.RestClient
import no.nav.aap.httpclient.request.PostRequest
import no.nav.aap.httpclient.tokenprovider.OidcToken
import no.nav.aap.httpclient.tokenprovider.azurecc.OnBehalfOfTokenProvider
import no.nav.aap.pdl.PdlResponseHandler
import no.nav.aap.requiredConfigForKey
import java.net.URI

object SafGateway {
    private val url = URI.create(requiredConfigForKey("integrasjon.saf.url"))

    val config = ClientConfig(
        scope = requiredConfigForKey("integrasjon.saf.scope"),
    )

    private val client = RestClient(
        config = config,
        tokenProvider = OnBehalfOfTokenProvider,
        errorHandler = PdlResponseHandler(config = config)
    )

    private fun query(request: SafRequest, currentToken: OidcToken): String {
        val httpRequest = PostRequest(body = request, currentToken = currentToken)
        return requireNotNull(client.post(uri = url, request = httpRequest, mapper = ::noParsing))
    }

    fun hentDokumenterForSak(saksnummer: Saksnummer, currentToken: OidcToken): String {
        val request = SafRequest(dokumentOversiktQuery.asQuery(), SafRequest.Variables(saksnummer.toString()))
        val response = query(request, currentToken)

        return response
    }
}

fun noParsing(s: String): String {
    return s
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
      sak {
        datoOpprettet
        fagsakId
        fagsaksystem
        sakstype
        tema
      } 
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
          filnavn
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