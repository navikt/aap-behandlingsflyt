package no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters

import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.prometheus
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.exception.InternfeilException
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.OnBehalfOfTokenProvider
import org.slf4j.LoggerFactory
import java.net.URI
import java.time.LocalDateTime

object SafListDokumentGateway {

    private val log = LoggerFactory.getLogger(javaClass)
    private val graphqlUrl = URI.create(requiredConfigForKey("integrasjon.saf.url.graphql"))

    val config = ClientConfig(
        scope = requiredConfigForKey("integrasjon.saf.scope"),
    )

    private val client = RestClient(
        config = config,
        tokenProvider = OnBehalfOfTokenProvider,
        responseHandler = SafResponseHandler(),
        prometheus = prometheus
    )

    private fun query(request: SafRequest, currentToken: OidcToken): SafDokumentoversiktFagsakDataResponse {
        val httpRequest = PostRequest(body = request, currentToken = currentToken)
        return requireNotNull(client.post(uri = graphqlUrl, request = httpRequest))
    }

    fun hentDokumenterForSak(saksnummer: Saksnummer, currentToken: OidcToken): List<SafListDokument> {
        val request = SafRequest(
            query = getQuery("/saf/dokumentoversiktFagsak.graphql"),
            variables = DokumentoversiktFagsakVariables(saksnummer.toString())
        )

        val response = query(request, currentToken)

        val dokumentoversiktFagsak = response.data?.dokumentoversiktFagsak ?: return emptyList()

        return dokumentoversiktFagsak.journalposter.flatMap { journalpost ->
            log.debug("Original Journalpost:  {}", journalpost)
            journalpost.dokumenter.flatMap { dok ->
                dok.dokumentvarianter
                    .filter { it.variantformat === Variantformat.ARKIV }
                    .map {
                        SafListDokument(
                            journalpostId = journalpost.journalpostId,
                            dokumentInfoId = dok.dokumentInfoId,
                            tittel = dok.tittel,
                            brevkode = dok.brevkode,
                            variantformat = it.variantformat,
                            erUtgående = journalpost.journalposttype == Journalposttype.U,
                            datoOpprettet = if (journalpost.datoOpprettet != null) {
                                journalpost.datoOpprettet
                            } else {
                                journalpost.relevanteDatoer?.first { it.datotype == "DATO_JOURNALFOERT" }?.dato
                            }!!
                        )
                    }
            }
        }.distinctBy { it.dokumentInfoId }
    }

    fun hentDokumenterForBruker(bruker: String, currentToken: OidcToken): List<Journalpost> {
        // TODO: Støtte filtrering fra frontend
        val request = SafRequest(
            query = getQuery("/saf/dokumentoversiktBruker.graphql"),
            variables = DokumentoversiktBrukerVariables(
                brukerId = BrukerId(bruker, "FNR"),
                tema = listOf("AAP"),
                journalposttyper = emptyList(),
                journalstatuser = emptyList(),
                foerste = 100
            )
        )

        val httpRequest = PostRequest(body = request, currentToken = currentToken)

        val response: SafDokumentoversiktBrukerDataResponse =
            requireNotNull(client.post(uri = graphqlUrl, request = httpRequest))

        return response.data?.dokumentoversiktBruker?.journalposter ?: emptyList()
    }

    private fun getQuery(name: String): String {
        val resource = javaClass.getResource(name)
            ?: throw InternfeilException("Kunne ikke opprette spørring mot SAF")

        return resource.readText().replace(Regex("[\n\t]"), "")
    }
}

data class SafListDokument(
    val dokumentInfoId: String,
    val journalpostId: String,
    val brevkode: String?,
    val tittel: String,
    val erUtgående: Boolean,
    val datoOpprettet: LocalDateTime,
    val variantformat: Variantformat
)
