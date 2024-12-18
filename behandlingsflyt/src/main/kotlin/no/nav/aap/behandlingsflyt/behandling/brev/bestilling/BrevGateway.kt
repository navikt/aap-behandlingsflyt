package no.nav.aap.behandlingsflyt.behandling.brev.bestilling

import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.brev.kontrakt.BestillBrevRequest
import no.nav.aap.brev.kontrakt.BestillBrevResponse
import no.nav.aap.brev.kontrakt.Brev
import no.nav.aap.brev.kontrakt.BrevbestillingResponse
import no.nav.aap.brev.kontrakt.Brevtype
import no.nav.aap.brev.kontrakt.FerdigstillBrevRequest
import no.nav.aap.brev.kontrakt.Språk
import no.nav.aap.brev.kontrakt.Vedlegg
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.error.BadRequestHttpResponsException
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.put
import no.nav.aap.komponenter.httpklient.httpclient.request.GetRequest
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.request.PutRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.komponenter.httpklient.json.DefaultJsonMapper
import no.nav.aap.verdityper.dokument.JournalpostId
import org.slf4j.LoggerFactory
import java.net.URI

class BrevGateway : BrevbestillingGateway {

    private val log = LoggerFactory.getLogger(BrevGateway::class.java)

    private val baseUri = URI.create(requiredConfigForKey("integrasjon.brev.url"))
    val config = ClientConfig(
        scope = requiredConfigForKey("integrasjon.brev.scope"),
    )

    private val client = RestClient.withDefaultResponseHandler(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider,
    )

    override fun bestillBrev(
        saksnummer: Saksnummer,
        behandlingReferanse: BehandlingReferanse,
        typeBrev: TypeBrev,
        vedlegg: Vedlegg?
    ): BrevbestillingReferanse {
        // TODO språk
        val request = BestillBrevRequest(
            saksnummer = saksnummer.toString(),
            behandlingReferanse = behandlingReferanse.referanse,
            brevtype = mapTypeBrev(typeBrev),
            sprak = Språk.NB,
            vedlegg = vedlegg?.let { setOf(it) }?: setOf()
        )

        val httpRequest = PostRequest<BestillBrevRequest>(
            body = request,
            additionalHeaders = listOf(
                Header("Accept", "application/json")
            )
        )

        val url = baseUri.resolve("/api/bestill")

        val response: BestillBrevResponse = requireNotNull(
            client.post(
                uri = url,
                request = httpRequest,
                mapper = { body, _ ->
                    DefaultJsonMapper.fromJson(body)
                })
        )

        return BrevbestillingReferanse(response.referanse)
    }

    override fun ferdigstill(referanse: BrevbestillingReferanse): Boolean {
        val url = baseUri.resolve("/api/ferdigstill")

        val request = PostRequest<FerdigstillBrevRequest>(
            body = FerdigstillBrevRequest(referanse.brevbestillingReferanse),
            additionalHeaders = listOf(
                Header("Accept", "application/json")
            )
        )

        try {
            client.post<_, Unit>(
                uri = url,
                request = request
            )
        } catch (_: BadRequestHttpResponsException) {
            log.warn("Bad request i response for ferdigstilling av brev.")
            return false
        }

        return true
    }

    override fun hent(bestillingReferanse: BrevbestillingReferanse): BrevbestillingResponse {
        val url = baseUri.resolve("/api/bestilling/$bestillingReferanse")
        val request = GetRequest(
            additionalHeaders = listOf(
                Header("Accept", "application/json")
            )
        )
        val response: BrevbestillingResponse = requireNotNull(
            client.get(
                uri = url, request = request, mapper = { body, _ ->
                    DefaultJsonMapper.fromJson(body)
                })
        )

        return response
    }

    override fun oppdater(bestillingReferanse: BrevbestillingReferanse, brev: Brev) {
        val url = baseUri.resolve("/api/bestilling/$bestillingReferanse/oppdater")

        val request = PutRequest<Brev>(body = brev)

        client.put<_, Unit>(url, request)
    }

    private fun mapTypeBrev(typeBrev: TypeBrev): Brevtype = when (typeBrev) {
        TypeBrev.VEDTAK_AVSLAG -> Brevtype.AVSLAG
        TypeBrev.VEDTAK_INNVILGELSE -> Brevtype.INNVILGELSE
        TypeBrev.VARSEL_OM_BESTILLING -> Brevtype.VARSEL_OM_BESTILLING
    }
}
