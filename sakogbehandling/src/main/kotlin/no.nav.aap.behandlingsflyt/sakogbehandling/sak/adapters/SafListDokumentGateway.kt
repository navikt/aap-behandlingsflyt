package no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.OnBehalfOfTokenProvider
import no.nav.aap.saf.Journalposttype
import no.nav.aap.saf.SafDokumentoversiktFagsakDataResponse
import no.nav.aap.saf.SafRequest
import no.nav.aap.saf.SafResponseHandler
import no.nav.aap.saf.Variantformat
import org.slf4j.LoggerFactory
import java.net.URI
import java.time.LocalDateTime

private val log = LoggerFactory.getLogger(SafListDokumentGateway::class.java)

object SafListDokumentGateway {
    private val graphqlUrl = URI.create(requiredConfigForKey("integrasjon.saf.url.graphql"))

    val config = ClientConfig(
        scope = requiredConfigForKey("integrasjon.saf.scope"),
    )

    private val client = RestClient(
        config = config,
        tokenProvider = OnBehalfOfTokenProvider,
        responseHandler = SafResponseHandler()
    )

    private fun query(request: SafRequest, currentToken: OidcToken): SafDokumentoversiktFagsakDataResponse {
        val httpRequest = PostRequest(body = request, currentToken = currentToken)
        return requireNotNull(client.post(uri = graphqlUrl, request = httpRequest))
    }

    fun hentDokumenterForSak(saksnummer: Saksnummer, currentToken: OidcToken): List<Dokument> {
        val request = SafRequest(dokumentOversiktQuery.asQuery(), SafRequest.Variables(saksnummer.toString()))
        val response = query(request, currentToken)

        val dokumentoversiktFagsak = response.data?.dokumentoversiktFagsak ?: return emptyList()

        return dokumentoversiktFagsak.journalposter.flatMap { journalpost ->
            log.info("Original Journalpost:  $journalpost")
            journalpost.dokumenter.flatMap { dok ->
                dok.dokumentvarianter
                    .filter { it.variantformat === Variantformat.ARKIV }
                    .map {
                        Dokument(
                            journalpostId = journalpost.journalpostId,
                            dokumentInfoId = dok.dokumentInfoId,
                            tittel = dok.tittel,
                            brevkode = dok.brevkode,
                            variantformat = it.variantformat,
                            erUtgående = journalpost.journalposttype == Journalposttype.U,
                            datoOpprettet = journalpost.datoOpprettet
                        )
                    }
            }
        }
    }
}

data class Dokument(
    val dokumentInfoId: String,
    val journalpostId: String,
    val brevkode: String?,
    val tittel: String,
    val erUtgående: Boolean,
    val datoOpprettet: LocalDateTime,
    val variantformat: Variantformat
)

fun String.asQuery() = this.replace("\n", "")

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
      journalposttype
      behandlingstema
      relevanteDatoer {
        dato
        datotype
      }
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
